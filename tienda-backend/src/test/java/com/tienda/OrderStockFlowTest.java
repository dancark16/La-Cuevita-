package com.tienda;

import com.tienda.inventory.Inventory;
import com.tienda.inventory.InventoryMovementRepository;
import com.tienda.inventory.MovementType;
import com.tienda.orders.Coupon;
import com.tienda.orders.CouponRepository;
import com.tienda.orders.DiscountType;
import com.tienda.orders.OrderTransferService;
import com.tienda.products.variant.ProductVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujos de dinero y stock: comprar, apartar, pagar, cancelar, vencer, enviar y borrar. */
class OrderStockFlowTest extends IntegrationTestBase {

    @Autowired private OrderTransferService transferService;
    @Autowired private InventoryMovementRepository movements;
    @Autowired private CouponRepository coupons;

    private void assertStock(ProductVariant variant, int onHand, int reserved) {
        Inventory inventory = stockOf(variant);
        assertThat(inventory.getQuantityOnHand()).as("stock en mano").isEqualTo(onHand);
        assertThat(inventory.getQuantityReserved()).as("stock reservado").isEqualTo(reserved);
    }

    // ------------------------------------------------------------ reserva al comprar

    @Test
    @DisplayName("Comprar aparta las unidades pero todavía no descuenta el stock")
    void checkoutReservesStock() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();

        UUID orderId = placeOrder(token, variant, 2);

        assertStock(variant, 5, 2);
        JsonNodeAssert.status(orderAsAdmin(orderId), "PENDING");
    }

    @Test
    @DisplayName("No se puede comprar más de lo disponible y no queda nada apartado")
    void checkoutRejectsOverselling() throws Exception {
        ProductVariant variant = newVariant(2, "20.00");

        checkout(newCustomerToken(), null, List.of(item(variant, 3))).andExpect(status().isConflict());

        assertStock(variant, 2, 0);
    }

    @Test
    @DisplayName("Un pedido con varios productos es todo o nada: si uno falla, no se aparta ninguno")
    void checkoutIsAllOrNothing() throws Exception {
        ProductVariant enough = newVariant(10, "10.00");
        ProductVariant scarce = newVariant(1, "10.00");

        checkout(newCustomerToken(), null, List.of(item(enough, 2), item(scarce, 5)))
                .andExpect(status().isConflict());

        assertStock(enough, 10, 0);
        assertStock(scarce, 1, 0);
    }

    @Test
    @DisplayName("Otra clienta no puede comprar lo que ya está apartado")
    void reservedUnitsAreNotAvailableToOthers() throws Exception {
        ProductVariant variant = newVariant(3, "20.00");

        placeOrder(newCustomerToken(), variant, 3);

        checkout(newCustomerToken(), null, List.of(item(variant, 1))).andExpect(status().isConflict());
        assertStock(variant, 3, 3);
    }

    @Test
    @DisplayName("Dos compras a la vez por la última unidad: solo una gana")
    void concurrentPurchasesNeverOversell() throws Exception {
        ProductVariant variant = newVariant(1, "20.00");
        String tokenA = newCustomerToken();
        String tokenB = newCustomerToken();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> a = () -> checkout(tokenA, null, List.of(item(variant, 1))).andReturn().getResponse().getStatus();
            Callable<Integer> b = () -> checkout(tokenB, null, List.of(item(variant, 1))).andReturn().getResponse().getStatus();
            Future<Integer> fa = pool.submit(a);
            Future<Integer> fb = pool.submit(b);
            List<Integer> results = List.of(fa.get(), fb.get());

            assertThat(results).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdown();
        }
        assertStock(variant, 1, 1);
    }

    @Test
    @DisplayName("El precio promocional es el que se cobra")
    void promoPriceIsUsed() throws Exception {
        ProductVariant variant = newVariant(5, "30.00");
        variant.setPromoPrice(new BigDecimal("25.00"));
        variants.save(variant);

        String response = checkout(newCustomerToken(), null, List.of(item(variant, 2)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();

        assertThat(json(response).get("total").decimalValue()).isEqualByComparingTo("50.00");
    }

    // ------------------------------------------------------------ cupones

    @Test
    @DisplayName("Un cupón de porcentaje descuenta del total")
    void percentageCouponDiscounts() throws Exception {
        ProductVariant variant = newVariant(5, "40.00");
        String code = "P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        coupons.save(Coupon.builder().code(code).discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10")).validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(1))).build());

        String response = checkout(newCustomerToken(), code, List.of(item(variant, 2)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();

        assertThat(json(response).get("subtotal").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(json(response).get("discount").decimalValue()).isEqualByComparingTo("8.00");
        assertThat(json(response).get("total").decimalValue()).isEqualByComparingTo("72.00");
    }

    @Test
    @DisplayName("Un cupón vencido se rechaza y no aparta stock")
    void expiredCouponIsRejected() throws Exception {
        ProductVariant variant = newVariant(5, "40.00");
        String code = "V" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        coupons.save(Coupon.builder().code(code).discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("5")).validFrom(Instant.now().minus(Duration.ofDays(10)))
                .validUntil(Instant.now().minus(Duration.ofDays(1))).build());

        checkout(newCustomerToken(), code, List.of(item(variant, 1))).andExpect(status().isBadRequest());

        assertStock(variant, 5, 0);
    }

    @Test
    @DisplayName("Un descuento fijo nunca deja el total en negativo")
    void fixedCouponNeverGoesNegative() throws Exception {
        ProductVariant variant = newVariant(5, "10.00");
        String code = "F" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        coupons.save(Coupon.builder().code(code).discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("500")).validFrom(Instant.now().minus(Duration.ofDays(1)))
                .validUntil(Instant.now().plus(Duration.ofDays(1))).build());

        String response = checkout(newCustomerToken(), code, List.of(item(variant, 1)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();

        assertThat(json(response).get("total").decimalValue()).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------ pago

    @Test
    @DisplayName("Al confirmar el pago, la reserva se vuelve venta: baja el stock y queda registrado")
    void confirmPaymentTurnsReservationIntoSale() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 2);
        uploadReceipt(token, orderId).andExpect(status().is2xxSuccessful());

        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());

        assertStock(variant, 3, 0);
        JsonNodeAssert.status(orderAsAdmin(orderId), "PAID");
        long sales = movements.findAll().stream()
                .filter(m -> orderId.equals(m.getOrderId()) && m.getType() == MovementType.VENTA).count();
        assertThat(sales).isEqualTo(1);
    }

    @Test
    @DisplayName("Confirmar dos veces el mismo pago no descuenta el stock dos veces")
    void confirmPaymentIsIdempotent() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 2);

        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());
        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());

        assertStock(variant, 3, 0);
        long sales = movements.findAll().stream()
                .filter(m -> orderId.equals(m.getOrderId()) && m.getType() == MovementType.VENTA).count();
        assertThat(sales).isEqualTo(1);
    }

    @Test
    @DisplayName("Una clienta no puede confirmar pagos (solo el administrador)")
    void customerCannotConfirmPayment() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 1);

        mvc.perform(post("/api/admin/orders/" + orderId + "/confirm-payment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        assertStock(variant, 5, 1);
    }

    @Test
    @DisplayName("Rechazar el comprobante no toca el stock apartado")
    void rejectingReceiptKeepsReservation() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 2);
        uploadReceipt(token, orderId).andExpect(status().is2xxSuccessful());

        adminPost("/api/admin/orders/" + orderId + "/reject-receipt", Map.of("note", "Monto incorrecto"))
                .andExpect(status().isOk());

        assertStock(variant, 5, 2);
        JsonNodeAssert.status(orderAsAdmin(orderId), "PENDING");
    }

    // ------------------------------------------------------------ cancelar y vencer

    @Test
    @DisplayName("Si la clienta cancela, las unidades vuelven a estar disponibles")
    void customerCancelReleasesStock() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 2);

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertStock(variant, 5, 0);
        JsonNodeAssert.status(orderAsAdmin(orderId), "CANCELLED");
    }

    @Test
    @DisplayName("Si el administrador cancela, las unidades vuelven a estar disponibles")
    void adminCancelReleasesStock() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        UUID orderId = placeOrder(newCustomerToken(), variant, 2);

        adminPost("/api/admin/orders/" + orderId + "/cancel", Map.of("note", "Sin pago")).andExpect(status().isOk());

        assertStock(variant, 5, 0);
    }

    @Test
    @DisplayName("Cancelar dos veces no libera de más")
    void cancelTwiceDoesNotReleaseTwice() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String tokenA = newCustomerToken();
        UUID first = placeOrder(tokenA, variant, 2);
        placeOrder(newCustomerToken(), variant, 1); // otra clienta tiene 1 apartada

        mvc.perform(post("/api/orders/" + first + "/cancel").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mvc.perform(post("/api/orders/" + first + "/cancel").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());

        assertStock(variant, 5, 1);
    }

    @Test
    @DisplayName("Un pedido pagado ya no se puede cancelar")
    void paidOrderCannotBeCancelled() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 2);
        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        adminPost("/api/admin/orders/" + orderId + "/cancel", Map.of("note", "x")).andExpect(status().isConflict());

        assertStock(variant, 3, 0);
    }

    @Test
    @DisplayName("Cuando vence la reserva sin pago, el pedido se cancela y libera el stock")
    void expiredReservationReleasesStock() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        UUID orderId = placeOrder(newCustomerToken(), variant, 2);

        transferService.expire(orderId);

        assertStock(variant, 5, 0);
        JsonNodeAssert.status(orderAsAdmin(orderId), "CANCELLED");
    }

    @Test
    @DisplayName("Vencer un pedido ya pagado no hace nada")
    void expiringPaidOrderDoesNothing() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        UUID orderId = placeOrder(newCustomerToken(), variant, 2);
        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());

        transferService.expire(orderId);

        assertStock(variant, 3, 0);
        JsonNodeAssert.status(orderAsAdmin(orderId), "PAID");
    }

    // ------------------------------------------------------------ envío

    @Test
    @DisplayName("No se puede enviar un pedido que no está pagado")
    void cannotShipUnpaidOrder() throws Exception {
        UUID orderId = placeOrder(newCustomerToken(), newVariant(5, "20.00"), 1);

        adminPost("/api/admin/orders/" + orderId + "/ship", Map.of("trackingNumber", "ABC123"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Ciclo completo: comprar, pagar, enviar con guía y entregar")
    void fullLifecycle() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        String token = newCustomerToken();
        UUID orderId = placeOrder(token, variant, 2);
        uploadReceipt(token, orderId).andExpect(status().is2xxSuccessful());

        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());
        adminPost("/api/admin/orders/" + orderId + "/deliver").andExpect(status().isConflict()); // aún no se envió

        adminPost("/api/admin/orders/" + orderId + "/ship", Map.of("trackingNumber", "GUIA-777"))
                .andExpect(status().isOk());
        var shipped = orderAsAdmin(orderId);
        JsonNodeAssert.status(shipped, "SHIPPED");
        assertThat(shipped.get("tracking").get("trackingNumber").asText()).isEqualTo("GUIA-777");

        adminPost("/api/admin/orders/" + orderId + "/deliver").andExpect(status().isOk());
        JsonNodeAssert.status(orderAsAdmin(orderId), "DELIVERED");

        // La clienta ve su pedido entregado y el stock quedó descontado una sola vez
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertStock(variant, 3, 0);
    }

    // ------------------------------------------------------------ borrar y permisos

    @Test
    @DisplayName("Borrar un pedido pagado devuelve el stock vendido")
    void deletingPaidOrderRestoresStock() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        UUID orderId = placeOrder(newCustomerToken(), variant, 2);
        adminPost("/api/admin/orders/" + orderId + "/confirm-payment").andExpect(status().isOk());
        assertStock(variant, 3, 0);

        asAdmin(delete("/api/admin/orders/" + orderId)).andExpect(status().is2xxSuccessful());

        assertStock(variant, 5, 0);
        asAdmin(get("/api/admin/orders/" + orderId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Borrar un pedido pendiente libera lo apartado")
    void deletingPendingOrderReleasesReservation() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        UUID orderId = placeOrder(newCustomerToken(), variant, 2);

        asAdmin(delete("/api/admin/orders/" + orderId)).andExpect(status().is2xxSuccessful());

        assertStock(variant, 5, 0);
    }

    @Test
    @DisplayName("Una clienta no puede ver ni cancelar el pedido de otra")
    void customersCannotTouchOthersOrders() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        UUID orderId = placeOrder(newCustomerToken(), variant, 1);
        String stranger = newCustomerToken();

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());

        assertStock(variant, 5, 1);
    }

    @Test
    @DisplayName("Sin iniciar sesión no se puede comprar")
    void anonymousCannotCheckout() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------ inventario del administrador

    @Test
    @DisplayName("El administrador no puede bajar el stock por debajo de lo que hay apartado")
    void adminCannotLowerStockBelowReserved() throws Exception {
        ProductVariant variant = newVariant(5, "20.00");
        placeOrder(newCustomerToken(), variant, 4);

        adminPost("/api/inventory/movements", Map.of(
                "variantId", variant.getId().toString(), "type", "SALIDA", "quantity", 3, "reason", "Merma"))
                .andExpect(status().isConflict());
        assertStock(variant, 5, 4);

        adminPost("/api/inventory/movements", Map.of(
                "variantId", variant.getId().toString(), "type", "SALIDA", "quantity", 1, "reason", "Merma"))
                .andExpect(status().isCreated());
        assertStock(variant, 4, 4);
    }
}

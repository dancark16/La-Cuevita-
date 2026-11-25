package com.tienda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.categories.Category;
import com.tienda.categories.CategoryRepository;
import com.tienda.inventory.Inventory;
import com.tienda.inventory.InventoryRepository;
import com.tienda.inventory.InventoryService;
import com.tienda.products.Product;
import com.tienda.products.ProductRepository;
import com.tienda.products.ProductStatus;
import com.tienda.products.variant.Color;
import com.tienda.products.variant.ColorRepository;
import com.tienda.products.variant.ProductVariant;
import com.tienda.products.variant.ProductVariantRepository;
import com.tienda.products.variant.Size;
import com.tienda.products.variant.SizeRepository;
import com.tienda.products.variant.VariantStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de punta a punta: levanta la aplicación completa (seguridad, JWT, Flyway)
 * contra la base «tienda_test» y llama a la API como lo haría la tienda.
 * La base de pruebas se crea sola la primera vez (usa el mismo PostgreSQL de desarrollo).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String ADMIN_EMAIL = "admin-test@lacuevita.test";
    protected static final String ADMIN_PASSWORD = "AdminPrueba123";
    protected static final String CUSTOMER_PASSWORD = "ClientaPrueba123";

    private static final String HOST = env("POSTGRES_HOST", "localhost");
    private static final String PORT = env("POSTGRES_PORT", "5432");
    private static final String USER = env("POSTGRES_USER", "tienda_user");
    private static final String PASSWORD = env("POSTGRES_PASSWORD", "change_me_local");
    private static final String TEST_DB = "tienda_test";

    protected static final ObjectMapper JSON = new ObjectMapper();

    @Autowired protected MockMvc mvc;
    @Autowired protected CategoryRepository categories;
    @Autowired protected ProductRepository products;
    @Autowired protected SizeRepository sizes;
    @Autowired protected ColorRepository colors;
    @Autowired protected ProductVariantRepository variants;
    @Autowired protected InventoryRepository inventories;
    @Autowired protected InventoryService inventoryService;

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void ensureDatabase() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://" + HOST + ":" + PORT + "/postgres", USER, PASSWORD);
             Statement s = c.createStatement()) {
            var rs = s.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + TEST_DB + "'");
            if (!rs.next()) {
                s.execute("CREATE DATABASE " + TEST_DB);
            }
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        try {
            ensureDatabase();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar la base de pruebas " + TEST_DB
                    + ". ¿Está encendido PostgreSQL (docker compose up -d)?", e);
        }
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + HOST + ":" + PORT + "/" + TEST_DB);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    // ------------------------------------------------------------ datos de prueba

    /** Crea un producto con una variante activa y el stock indicado. */
    protected ProductVariant newVariant(int stock, String price) {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Category category = categories.save(Category.builder().name("Cat " + tag).slug("cat-" + tag).build());
        Product product = products.save(Product.builder()
                .category(category).name("Producto " + tag).baseStatus(ProductStatus.ACTIVE).build());
        Size size = sizes.save(Size.builder().label(tag.substring(0, 6)).build());
        Color color = colors.save(Color.builder().name("Color " + tag).hex("#112233").build());
        ProductVariant variant = variants.save(ProductVariant.builder()
                .product(product).size(size).color(color).sku("T-" + tag)
                .price(new BigDecimal(price)).status(VariantStatus.ACTIVE).build());
        inventoryService.initializeForVariant(variant);
        Inventory inventory = inventories.findByVariantId(variant.getId()).orElseThrow();
        inventory.setQuantityOnHand(stock);
        inventories.save(inventory);
        return variant;
    }

    protected Inventory stockOf(ProductVariant variant) {
        return inventories.findByVariantId(variant.getId()).orElseThrow();
    }

    /** Registra una clienta nueva y devuelve su token. */
    protected String newCustomerToken() throws Exception {
        String email = "clienta-" + UUID.randomUUID().toString().substring(0, 8) + "@lacuevita.test";
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("email", email, "password", CUSTOMER_PASSWORD))))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return json(response).get("accessToken").asText();
    }

    protected String adminToken() throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json(response).get("accessToken").asText();
    }

    // ------------------------------------------------------------ llamadas a la API

    protected static Map<String, Object> item(ProductVariant variant, int quantity) {
        return Map.of("variantId", variant.getId().toString(), "quantity", quantity);
    }

    protected ResultActions checkout(String token, String couponCode, List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("customerProfile", Map.of("idType", "CEDULA", "idNumber", uniqueIdNumber(token),
                "firstName", "Ana", "lastName", "Prueba", "phone", "0999999999"));
        body.put("address", Map.of("line1", "Av. Siempre Viva 123", "city", "Quito", "province", "Pichincha"));
        body.put("items", items);
        if (couponCode != null) {
            body.put("couponCode", couponCode);
        }
        return mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    /** La misma clienta siempre usa la misma cédula; clientas distintas, cédulas distintas. */
    private static String uniqueIdNumber(String token) {
        return String.format("%010d", Math.abs((long) token.hashCode()) % 10_000_000_000L);
    }

    /** Hace un pedido y devuelve su id (falla la prueba si no se pudo crear). */
    protected UUID placeOrder(String token, ProductVariant variant, int quantity) throws Exception {
        String response = checkout(token, null, List.of(item(variant, quantity)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json(response).get("id").asText());
    }

    protected ResultActions uploadReceipt(String token, UUID orderId) throws Exception {
        byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile("file", "comprobante.pdf", "application/pdf", pdf);
        return mvc.perform(multipart("/api/orders/" + orderId + "/receipt").file(file)
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + adminToken()));
    }

    protected ResultActions adminPost(String path) throws Exception {
        return asAdmin(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    protected ResultActions adminPost(String path, Object body) throws Exception {
        return asAdmin(post(path).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    protected JsonNode orderAsAdmin(UUID id) throws Exception {
        return json(asAdmin(get("/api/admin/orders/" + id)).andReturn().getResponse().getContentAsString());
    }

    protected static JsonNode json(String text) throws Exception {
        return JSON.readTree(text);
    }
}

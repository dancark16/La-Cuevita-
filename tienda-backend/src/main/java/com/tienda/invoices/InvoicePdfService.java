package com.tienda.invoices;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tienda.customers.Customer;
import com.tienda.orders.Order;
import com.tienda.settings.StoreSettings;
import com.tienda.settings.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Genera el PDF de la factura con el formato del RIDE del SRI y el logo de La Cuevita.
 *
 * Mientras la factura no venga de un proveedor autorizado (clave de acceso de 49 dígitos),
 * el documento lo dice con un aviso: no se inventan números de autorización.
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final StoreSettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public byte[] render(Invoice invoice) {
        String html = buildHtml(invoice);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura", e);
        }
    }

    // ------------------------------------------------------------------ HTML

    private String buildHtml(Invoice invoice) {
        Order order = invoice.getOrder();
        Customer customer = order.getCustomer();
        StoreSettings st = settingsRepository.findById((short) 1).orElseGet(StoreSettings::new);

        boolean authorized = invoice.getAccessKey() != null && invoice.getAccessKey().matches("\\d{49}");
        String number = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "—";
        String issued = DATE_TIME.format(invoice.getCreatedAt().atZone(ZONE));
        String issuedDate = DATE.format(invoice.getCreatedAt().atZone(ZONE));

        String clientName = customer.getBusinessName() != null && !customer.getBusinessName().isBlank()
                ? customer.getBusinessName()
                : ((customer.getFirstName() == null ? "" : customer.getFirstName()) + " "
                + (customer.getLastName() == null ? "" : customer.getLastName())).trim();

        BigDecimal total = invoice.getTotal();
        BigDecimal tax = invoice.getTaxAmount();
        BigDecimal subtotal = total.subtract(tax);

        BigDecimal rate = invoice.getItems().isEmpty() ? new BigDecimal("0.15") : invoice.getItems().get(0).getTaxRate();
        BigDecimal divisor = BigDecimal.ONE.add(rate);
        BigDecimal linesNet = BigDecimal.ZERO;
        StringBuilder rows = new StringBuilder();
        int line = 1;
        for (InvoiceItem item : invoice.getItems()) {
            BigDecimal unitNet = item.getUnitPrice().divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal lineNet = item.getSubtotal().divide(divisor, 2, RoundingMode.HALF_UP);
            linesNet = linesNet.add(lineNet);
            rows.append("<tr><td class='c'>").append(line++).append("</td><td class='c'>")
                    .append(item.getQuantity()).append(".00</td><td>").append(esc(item.getDescription()))
                    .append("</td><td class='r'>").append(money(unitNet)).append("</td><td class='r'>0.00</td><td class='r'>")
                    .append(money(lineNet)).append("</td></tr>");
        }
        BigDecimal discount = linesNet.subtract(subtotal).max(BigDecimal.ZERO);

        String razon = firstNonBlank(st.getBusinessName(), st.getTradeName(), "LA CUEVITA");
        String comercial = firstNonBlank(st.getTradeName(), razon);

        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='UTF-8'/><style>")
                .append("@page { size: A4; margin: 14mm 12mm; }")
                .append("body { font-family: Helvetica, Arial, sans-serif; font-size: 8.5pt; color: #111; }")
                .append("table { border-collapse: collapse; width: 100%; }")
                .append(".box { border: 1px solid #222; border-radius: 6px; padding: 8px 10px; }")
                .append(".lbl { color: #333; }")
                .append(".big { font-size: 12pt; font-weight: bold; }")
                .append(".grid td, .grid th { border: 1px solid #222; padding: 4px 5px; }")
                .append(".grid th { background: #eef4fb; font-weight: normal; text-align: center; }")
                .append(".c { text-align: center; } .r { text-align: right; }")
                .append(".tot td { border: 1px solid #222; padding: 4px 6px; }")
                .append(".muted { color: #555; }")
                .append("</style></head><body>");

        // ---- cabecera: logo + emisor | recuadro de la factura
        h.append("<table><tr><td style='width:50%; vertical-align:top; padding-right:8px'>")
                .append("<img src='").append(logoDataUri()).append("' style='height:46px; margin-bottom:10px'/>")
                .append("<div class='box'>")
                .append("<div class='big'>").append(esc(razon)).append("</div>")
                .append("<div style='margin-top:4px'>").append(esc(comercial)).append("</div>")
                .append("<table style='margin-top:8px'>")
                .append(kv("Dirección Matriz:", st.getHeadOfficeAddress()))
                .append(kv("Dirección Sucursal:", st.getBranchAddress()))
                .append(kv("Contribuyente Especial:", st.getSpecialTaxpayer()))
                .append(kv("OBLIGADO A LLEVAR CONTABILIDAD:", st.isAccountingRequired() ? "SI" : "NO"))
                .append("</table></div></td>")
                .append("<td style='width:50%; vertical-align:top; padding-left:8px'><div class='box' style='min-height:230px'>")
                .append("<div><span class='big'>R.U.C.:</span> <span class='big'>").append(esc(orDash(st.getRuc()))).append("</span></div>")
                .append("<div class='big' style='margin-top:6px'>FACTURA</div>")
                .append("<div style='margin-top:6px'>No. <b>").append(esc(number)).append("</b></div>")
                .append("<div class='lbl' style='margin-top:8px'>NÚMERO DE AUTORIZACIÓN</div>")
                .append("<div><b>").append(authorized ? esc(invoice.getAccessKey()) : "PENDIENTE").append("</b></div>")
                .append("<table style='margin-top:8px'>")
                .append(kv("FECHA Y HORA DE AUTORIZACIÓN:", authorized ? issued : "—"))
                .append(kv("AMBIENTE:", authorized ? "PRODUCCIÓN" : "PRUEBAS"))
                .append(kv("EMISIÓN:", "NORMAL"))
                .append("</table>")
                .append("<div class='lbl' style='margin-top:8px'>CLAVE DE ACCESO</div>");
        if (authorized) {
            h.append("<img src='").append(barcodeDataUri(invoice.getAccessKey())).append("' style='width:100%; height:44px'/>")
                    .append("<div style='font-size:7pt; text-align:center'>").append(invoice.getAccessKey()).append("</div>");
        } else {
            h.append("<div class='muted'>Se asigna al emitir la factura electrónica.</div>");
        }
        h.append("</div></td></tr></table>");

        // ---- comprador
        h.append("<div class='box' style='margin-top:10px'><table>")
                .append(kv("Razón Social / Nombres y Apellidos:", clientName))
                .append(kv("Identificación:", customer.getIdNumber()))
                .append(kv("Fecha:", issuedDate))
                .append(kv("Dirección:", order.getAddress() == null ? null
                        : order.getAddress().getLine1() + ", " + order.getAddress().getCity() + ", " + order.getAddress().getProvince()))
                .append("</table></div>");

        // ---- detalle
        h.append("<table class='grid' style='margin-top:10px'><tr><th style='width:5%'>#</th><th style='width:9%'>Cantidad</th>")
                .append("<th>Descripción</th><th style='width:14%'>Precio Unitario</th><th style='width:11%'>Descuento</th>")
                .append("<th style='width:14%'>Precio Total</th></tr>").append(rows).append("</table>");

        // ---- información adicional + totales
        h.append("<table style='margin-top:8px'><tr><td style='width:55%; vertical-align:top; padding-right:10px'>")
                .append("<table class='grid'><tr><th colspan='2'>Información Adicional</th></tr>")
                .append(addRow("Correo electrónico", customer.getEmail()))
                .append(addRow("Teléfono", customer.getPhone()))
                .append(addRow("Pedido", "#" + order.getOrderNumber()))
                .append(addRow("Usuario generador", "SISTEMA"))
                .append("</table>")
                .append("<table class='grid' style='margin-top:10px'><tr><th>Forma de pago</th><th style='width:25%'>Valor</th></tr>")
                .append("<tr><td>20 - OTROS CON UTILIZACION DEL SISTEMA FINANCIERO (Transferencia)</td><td class='r'>")
                .append(money(total)).append("</td></tr></table>")
                .append("</td><td style='width:45%; vertical-align:top'><table class='tot'>")
                .append(totalRow("SUBTOTAL 15%", subtotal))
                .append(totalRow("SUBTOTAL NO OBJETO DE IVA", BigDecimal.ZERO))
                .append(totalRow("SUBTOTAL EXENTO DE IVA", BigDecimal.ZERO))
                .append(totalRow("SUBTOTAL SIN IMPUESTOS", subtotal))
                .append(totalRow("TOTAL DESCUENTO", discount))
                .append(totalRow("ICE", BigDecimal.ZERO))
                .append(totalRow("IVA 15%", tax))
                .append(totalRow("PROPINA", BigDecimal.ZERO))
                .append("<tr><td><b>VALOR TOTAL</b></td><td class='r'><b>").append(money(total)).append("</b></td></tr>")
                .append("</table></td></tr></table>");

        h.append("<p class='muted' style='margin-top:12px; font-size:7.5pt'>Gracias por tu compra en La Cuevita.</p>")
                .append("</body></html>");
        return h.toString();
    }

    private static String kv(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<tr><td class='lbl' style='padding:2px 8px 2px 0; width:44%; vertical-align:top'>" + esc(label)
                + "</td><td style='padding:2px 0'>" + esc(value) + "</td></tr>";
    }

    private static String addRow(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<tr><td style='width:38%'>" + esc(label) + "</td><td>" + esc(value) + "</td></tr>";
    }

    private static String totalRow(String label, BigDecimal value) {
        return "<tr><td>" + esc(label) + "</td><td class='r' style='width:30%'>" + money(value) + "</td></tr>";
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.US, "%.2f", value.setScale(2, RoundingMode.HALF_UP));
    }

    private static String orDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String logoDataUri() {
        try (var in = new ClassPathResource("brand/logo-dark.png").getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            return "";
        }
    }

    private static String barcodeDataUri(String value) {
        try {
            BitMatrix matrix = new Code128Writer().encode(value, BarcodeFormat.CODE_128, 900, 80);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
}

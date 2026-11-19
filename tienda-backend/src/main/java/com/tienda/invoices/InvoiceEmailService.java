package com.tienda.invoices;

import com.tienda.orders.Order;
import com.tienda.orders.OrderRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * Envía la factura por correo a la clienta: un mensaje con el diseño de La Cuevita y el PDF adjunto.
 * Si el correo no está configurado (MAIL_HOST vacío) no hace nada y lo deja en el log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceEmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfService pdfService;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String from;

    public boolean isConfigured() {
        return mailHost != null && !mailHost.isBlank() && mailSender.getIfAvailable() != null;
    }

    /** Programa el envío para cuando termine la transacción actual (así el pedido ya está guardado). */
    public void sendAfterCommit(UUID orderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAsync(orderId);
                }
            });
        } else {
            sendAsync(orderId);
        }
    }

    @Async
    public void sendAsync(UUID orderId) {
        try {
            send(orderId);
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar la factura del pedido {} por correo", orderId, e);
        }
    }

    /** Envía (o reenvía) la factura. Lanza excepción si el correo no está configurado o falla. */
    @Transactional(readOnly = true)
    public void send(UUID orderId) {
        if (!isConfigured()) {
            log.info("Correo sin configurar (MAIL_HOST): la factura del pedido {} no se envió", orderId);
            throw new IllegalStateException("El correo no está configurado en el servidor (MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD)");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("El pedido aún no tiene factura"));
        String to = order.getCustomer().getEmail();
        if (to == null || to.isBlank()) {
            throw new IllegalStateException("La clienta no tiene correo");
        }

        byte[] pdf = pdfService.render(invoice);
        String name = order.getCustomer().getFirstName() != null && !order.getCustomer().getFirstName().isBlank()
                ? order.getCustomer().getFirstName() : "";

        try {
            MimeMessage message = mailSender.getObject().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "La Cuevita");
            }
            helper.setTo(to);
            helper.setSubject("Tu factura de La Cuevita · Pedido #" + order.getOrderNumber());
            helper.setText(html(name, order, invoice), true);
            helper.addInline("logo", new ClassPathResource("brand/logo-light.png"), "image/png");
            helper.addAttachment("Factura-" + invoice.getInvoiceNumber() + ".pdf", new ByteArrayResource(pdf), "application/pdf");
            mailSender.getObject().send(message);
            log.info("Factura del pedido #{} enviada a {}", order.getOrderNumber(), to);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    private static String html(String name, Order order, Invoice invoice) {
        String total = String.format(Locale.US, "%.2f", invoice.getTotal().setScale(2, java.math.RoundingMode.HALF_UP));
        String hello = name.isBlank() ? "¡Gracias por tu compra!" : "¡Gracias por tu compra, " + InvoicePdfService.esc(name) + "!";
        return """
                <div style="margin:0;padding:24px 12px;background:#eaf5fc;font-family:Arial,Helvetica,sans-serif;color:#14284b">
                  <table role="presentation" align="center" width="100%%" style="max-width:560px;margin:0 auto;border-collapse:collapse">
                    <tr><td style="background:#1f3a93;padding:22px 24px;border-radius:14px 14px 0 0;border-bottom:4px solid #ff4fa3">
                      <img src="cid:logo" alt="La Cuevita" height="34" style="display:block;height:34px">
                    </td></tr>
                    <tr><td style="background:#ffffff;padding:28px 24px;border-radius:0 0 14px 14px;border:1px solid #cfe3f2;border-top:0">
                      <h1 style="margin:0 0 8px;font-size:22px;color:#14284b">%s</h1>
                      <p style="margin:0 0 18px;font-size:15px;line-height:1.55;color:#4a5d78">
                        Ya registramos tu pago. Te adjuntamos tu factura en PDF.
                      </p>
                      <table role="presentation" width="100%%" style="border-collapse:collapse;background:#f1f8fd;border:1px solid #cfe3f2;border-radius:10px">
                        <tr><td style="padding:12px 16px;font-size:13px;color:#4a5d78">Pedido</td>
                            <td style="padding:12px 16px;font-size:15px;font-weight:bold;text-align:right">#%s</td></tr>
                        <tr><td style="padding:12px 16px;font-size:13px;color:#4a5d78;border-top:1px solid #cfe3f2">Factura</td>
                            <td style="padding:12px 16px;font-size:15px;font-weight:bold;text-align:right;border-top:1px solid #cfe3f2">%s</td></tr>
                        <tr><td style="padding:12px 16px;font-size:13px;color:#4a5d78;border-top:1px solid #cfe3f2">Total pagado</td>
                            <td style="padding:12px 16px;font-size:18px;font-weight:bold;text-align:right;color:#e6338a;border-top:1px solid #cfe3f2">$%s</td></tr>
                      </table>
                      <p style="margin:20px 0 0;font-size:13px;line-height:1.55;color:#4a5d78">
                        Si tienes alguna duda sobre tu pedido, respóndenos por WhatsApp. ¡Gracias por elegirnos!
                      </p>
                    </td></tr>
                    <tr><td style="padding:14px 8px;text-align:center;font-size:11px;color:#7a8ba3">
                      La Cuevita · Este correo se envió porque realizaste una compra en nuestra tienda.
                    </td></tr>
                  </table>
                </div>
                """.formatted(hello, order.getOrderNumber(), InvoicePdfService.esc(String.valueOf(invoice.getInvoiceNumber())), total);
    }
}

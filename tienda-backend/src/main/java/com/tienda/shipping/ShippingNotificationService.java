package com.tienda.shipping;

import com.tienda.orders.Order;
import com.tienda.orders.OrderRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/** Avisa por correo a la clienta cuando su pedido sale, cambia o se entrega. Sin correo configurado no hace nada. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingNotificationService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final OrderRepository orderRepository;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void notifyShippedAfterCommit(UUID orderId) {
        afterCommit(() -> sendAsync(orderId, null));
    }

    public void notifyUpdateAfterCommit(UUID orderId, String message) {
        afterCommit(() -> sendAsync(orderId, message));
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Async
    public void sendAsync(UUID orderId, String update) {
        try {
            send(orderId, update);
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar del envío del pedido {}", orderId, e);
        }
    }

    @Transactional(readOnly = true)
    public void send(UUID orderId, String update) {
        if (mailHost == null || mailHost.isBlank() || mailSender.getIfAvailable() == null) {
            log.info("Correo sin configurar: no se avisó del envío del pedido {}", orderId);
            return;
        }
        Order order = orderRepository.findById(orderId).orElseThrow();
        String to = order.getCustomer().getEmail();
        if (to == null || to.isBlank()) {
            return;
        }
        boolean first = update == null;
        String subject = first
                ? "Tu pedido #" + order.getOrderNumber() + " va en camino"
                : "Novedad de tu pedido #" + order.getOrderNumber();
        try {
            MimeMessage message = mailSender.getObject().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "La Cuevita");
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html(order, update), true);
            helper.addInline("logo", new ClassPathResource("brand/logo-light.png"), "image/png");
            mailSender.getObject().send(message);
            log.info("Aviso de envío del pedido #{} enviado a {}", order.getOrderNumber(), to);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    private String html(Order order, String update) {
        String title = update == null ? "¡Tu pedido va en camino!" : "Novedad de tu pedido";
        String intro = update == null
                ? "Ya despachamos tu pedido #" + order.getOrderNumber() + ". Aquí tienes los datos para seguirlo."
                : esc(update);
        StringBuilder rows = new StringBuilder();
        if (order.getCarrierName() != null) {
            rows.append(row("Transportista", esc(order.getCarrierName())));
        }
        if (order.getTrackingNumber() != null) {
            rows.append(row("Número de guía", esc(order.getTrackingNumber())));
        }
        String buttons = "";
        if (order.getTrackingUrl() != null) {
            buttons += button(order.getTrackingUrl(), "Rastrear mi paquete", "#ff4fa3");
        }
        buttons += button(frontendUrl.replaceAll("/+$", "") + "/pedido/" + order.getId(), "Ver mi pedido", "#1f3a93");
        return """
                <div style="margin:0;padding:24px 12px;background:#eaf5fc;font-family:Arial,Helvetica,sans-serif;color:#14284b">
                  <table role="presentation" align="center" width="100%%" style="max-width:560px;margin:0 auto;border-collapse:collapse">
                    <tr><td style="background:#1f3a93;padding:22px 24px;border-radius:14px 14px 0 0;border-bottom:4px solid #ff4fa3">
                      <img src="cid:logo" alt="La Cuevita" height="34" style="display:block;height:34px">
                    </td></tr>
                    <tr><td style="background:#ffffff;padding:28px 24px;border-radius:0 0 14px 14px;border:1px solid #cfe3f2;border-top:0">
                      <h1 style="margin:0 0 8px;font-size:22px;color:#14284b">%s</h1>
                      <p style="margin:0 0 18px;font-size:15px;line-height:1.55;color:#4a5d78">%s</p>
                      %s
                      <div style="margin-top:22px">%s</div>
                    </td></tr>
                    <tr><td style="padding:14px 8px;text-align:center;font-size:11px;color:#7a8ba3">
                      La Cuevita · Recibes este correo porque hiciste un pedido en nuestra tienda.
                    </td></tr>
                  </table>
                </div>
                """.formatted(title, intro,
                rows.length() == 0 ? "" : "<table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;background:#f1f8fd;border:1px solid #cfe3f2;border-radius:10px\">" + rows + "</table>",
                buttons);
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:12px 16px;font-size:13px;color:#4a5d78;border-bottom:1px solid #cfe3f2\">" + label
                + "</td><td style=\"padding:12px 16px;font-size:15px;font-weight:bold;text-align:right;border-bottom:1px solid #cfe3f2\">"
                + value + "</td></tr>";
    }

    private static String button(String url, String label, String color) {
        return "<a href=\"" + esc(url) + "\" style=\"display:inline-block;margin:0 8px 8px 0;padding:12px 20px;background:" + color
                + ";color:#ffffff;font-weight:bold;font-size:14px;text-decoration:none;border-radius:8px\">" + esc(label) + "</a>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

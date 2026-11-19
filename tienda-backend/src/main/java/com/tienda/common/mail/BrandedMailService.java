package com.tienda.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Envía correos sencillos con el diseño de La Cuevita: cabecera azul con logo, un mensaje y un botón. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandedMailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String from;

    public boolean isConfigured() {
        return mailHost != null && !mailHost.isBlank() && mailSender.getIfAvailable() != null;
    }

    /** Envía el correo. Lanza IllegalStateException si el correo no está configurado o falla. */
    public void send(String to, String subject, String title, String intro, String buttonLabel, String buttonUrl, String footer) {
        if (!isConfigured()) {
            throw new IllegalStateException("El correo no está configurado en el servidor");
        }
        try {
            MimeMessage message = mailSender.getObject().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "La Cuevita");
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html(title, intro, buttonLabel, buttonUrl, footer), true);
            helper.addInline("logo", new ClassPathResource("brand/logo-light.png"), "image/png");
            mailSender.getObject().send(message);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    private static String html(String title, String intro, String buttonLabel, String buttonUrl, String footer) {
        String button = buttonUrl == null ? "" : "<div style=\"margin-top:22px\"><a href=\"" + esc(buttonUrl)
                + "\" style=\"display:inline-block;padding:13px 24px;background:#ff4fa3;color:#ffffff;font-weight:bold;"
                + "font-size:15px;text-decoration:none;border-radius:8px\">" + esc(buttonLabel) + "</a></div>"
                + "<p style=\"margin:18px 0 0;font-size:12px;color:#7a8ba3;word-break:break-all\">Si el botón no funciona, copia este enlace en tu navegador:<br>"
                + esc(buttonUrl) + "</p>";
        return """
                <div style="margin:0;padding:24px 12px;background:#eaf5fc;font-family:Arial,Helvetica,sans-serif;color:#14284b">
                  <table role="presentation" align="center" width="100%%" style="max-width:560px;margin:0 auto;border-collapse:collapse">
                    <tr><td style="background:#1f3a93;padding:22px 24px;border-radius:14px 14px 0 0;border-bottom:4px solid #ff4fa3">
                      <img src="cid:logo" alt="La Cuevita" height="34" style="display:block;height:34px">
                    </td></tr>
                    <tr><td style="background:#ffffff;padding:28px 24px;border-radius:0 0 14px 14px;border:1px solid #cfe3f2;border-top:0">
                      <h1 style="margin:0 0 10px;font-size:22px;color:#14284b">%s</h1>
                      <p style="margin:0;font-size:15px;line-height:1.55;color:#4a5d78">%s</p>
                      %s
                    </td></tr>
                    <tr><td style="padding:14px 8px;text-align:center;font-size:11px;color:#7a8ba3">%s</td></tr>
                  </table>
                </div>
                """.formatted(esc(title), esc(intro), button, esc(footer == null ? "La Cuevita" : footer));
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

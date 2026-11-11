package com.tienda.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Valida un ID token de Google con el endpoint oficial tokeninfo.
 * Comprueba firma/expiración (lo hace Google), audiencia y correo verificado.
 */
@Component
public class GoogleTokenVerifier {

    public record GoogleIdentity(String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenInfo(String aud, String email, String email_verified) {
    }

    private final RestClient restClient = RestClient.create("https://oauth2.googleapis.com");
    private final String clientId;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public GoogleIdentity verify(String idToken) {
        if (clientId.isBlank()) {
            throw new InvalidGoogleTokenException("El inicio de sesión con Google no está configurado");
        }

        TokenInfo info;
        try {
            info = restClient.get()
                    .uri(uri -> uri.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(TokenInfo.class);
        } catch (RestClientException ex) {
            throw new InvalidGoogleTokenException("Token de Google inválido");
        }

        if (info == null
                || !clientId.equals(info.aud())
                || !"true".equals(info.email_verified())
                || info.email() == null) {
            throw new InvalidGoogleTokenException("Token de Google inválido");
        }
        return new GoogleIdentity(info.email().toLowerCase());
    }
}

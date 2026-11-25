package com.tienda;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ingreso, bloqueo por intentos fallidos y separación entre clientas y administrador. */
class AuthSecurityTest extends IntegrationTestBase {

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "password", password))));
    }

    @Test
    @DisplayName("El administrador se crea solo y puede ingresar")
    void adminBootstrapCanLogin() throws Exception {
        login(ADMIN_EMAIL, ADMIN_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una contraseña incorrecta se rechaza")
    void wrongPasswordIsRejected() throws Exception {
        login(ADMIN_EMAIL, "equivocada123").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos la cuenta se bloquea, incluso con la contraseña correcta")
    void accountLocksAfterRepeatedFailures() throws Exception {
        String email = "bloqueo-" + UUID.randomUUID().toString().substring(0, 8) + "@lacuevita.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "password", CUSTOMER_PASSWORD))))
                .andExpect(status().is2xxSuccessful());

        for (int i = 0; i < 5; i++) {
            login(email, "incorrecta" + i).andExpect(status().isUnauthorized());
        }

        login(email, CUSTOMER_PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("Un ingreso correcto reinicia el contador de fallos")
    void successfulLoginResetsFailures() throws Exception {
        String email = "reinicio-" + UUID.randomUUID().toString().substring(0, 8) + "@lacuevita.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "password", CUSTOMER_PASSWORD))));

        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 4; i++) {
                login(email, "incorrecta").andExpect(status().isUnauthorized());
            }
            login(email, CUSTOMER_PASSWORD).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Registrar un correo repetido da conflicto")
    void duplicateRegistrationIsRejected() throws Exception {
        String email = "repetida-" + UUID.randomUUID().toString().substring(0, 8) + "@lacuevita.test";
        String body = JSON.writeValueAsString(Map.of("email", email, "password", CUSTOMER_PASSWORD));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Las rutas del panel piden ser administrador")
    void adminRoutesRequireAdmin() throws Exception {
        mvc.perform(get("/api/admin/orders")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + newCustomerToken()))
                .andExpect(status().isForbidden());
        asAdmin(get("/api/admin/orders")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Recuperar contraseña responde igual exista o no el correo (no revela cuentas)")
    void forgotPasswordDoesNotRevealAccounts() throws Exception {
        for (String email : new String[]{ADMIN_EMAIL, "no-existe-" + UUID.randomUUID() + "@lacuevita.test"}) {
            mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                            .content(JSON.writeValueAsString(Map.of("email", email))))
                    .andExpect(status().isAccepted());
        }
    }
}

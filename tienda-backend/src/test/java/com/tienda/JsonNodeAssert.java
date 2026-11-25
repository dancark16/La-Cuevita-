package com.tienda;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/** Atajo para comprobar el estado de un pedido en la respuesta de la API. */
final class JsonNodeAssert {

    private JsonNodeAssert() {
    }

    static void status(JsonNode order, String expected) {
        assertThat(order.get("status").asText()).as("estado del pedido").isEqualTo(expected);
    }
}

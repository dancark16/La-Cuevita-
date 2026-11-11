package com.tienda.orders.dto;

import jakarta.validation.constraints.Size;

/** Motivo o nota que el administrador agrega al cambiar el estado de un pedido. */
public record OrderNoteRequest(@Size(max = 500) String note) {
}

package com.tienda.invoices;

public class InvalidInvoiceStateException extends RuntimeException {

    public InvalidInvoiceStateException(String message) {
        super(message);
    }
}

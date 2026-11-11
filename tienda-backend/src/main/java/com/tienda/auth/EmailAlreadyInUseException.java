package com.tienda.auth;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("El correo ya está registrado: " + email);
    }
}

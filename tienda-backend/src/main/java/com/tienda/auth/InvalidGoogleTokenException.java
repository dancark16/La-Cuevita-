package com.tienda.auth;

public class InvalidGoogleTokenException extends InvalidRefreshTokenException {

    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}

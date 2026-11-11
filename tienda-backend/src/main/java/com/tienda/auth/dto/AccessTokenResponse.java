package com.tienda.auth.dto;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

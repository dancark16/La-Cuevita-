package com.tienda.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validación fallida", details);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (10 MB por imagen).", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(com.tienda.auth.EmailAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleEmailInUse(com.tienda.auth.EmailAlreadyInUseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(com.tienda.auth.InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(com.tienda.auth.InvalidRefreshTokenException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", List.of());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "No se puede completar la operación: hay datos relacionados que lo impiden", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    @ExceptionHandler(com.tienda.inventory.InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(com.tienda.inventory.InsufficientStockException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(com.tienda.orders.InvalidOrderStateException.class)
    public ResponseEntity<ApiError> handleInvalidOrderState(com.tienda.orders.InvalidOrderStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(com.tienda.invoices.InvalidInvoiceStateException.class)
    public ResponseEntity<ApiError> handleInvalidInvoiceState(com.tienda.invoices.InvalidInvoiceStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(body);
    }
}

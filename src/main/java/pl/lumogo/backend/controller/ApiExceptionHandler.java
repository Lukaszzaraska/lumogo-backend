package pl.lumogo.backend.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.lumogo.backend.dto.AuthResponse;
import pl.lumogo.backend.service.EmailAlreadyUsedException;
import pl.lumogo.backend.service.UserNotFoundException;

/** Jeden format błędów dla całego API: { status: "error", code, message }. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({EmailAlreadyUsedException.class, DataIntegrityViolationException.class})
    ResponseEntity<AuthResponse> emailTaken() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(AuthResponse.error("EMAIL_TAKEN", new EmailAlreadyUsedException().getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<AuthResponse> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(AuthResponse.error("INVALID_CREDENTIALS", "Nieprawidłowy e-mail lub hasło"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<AuthResponse> userNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AuthResponse.error("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<AuthResponse> validation(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(AuthResponse.error("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<AuthResponse> unreadable() {
        return ResponseEntity.badRequest().body(AuthResponse.error("VALIDATION_ERROR", "Nieprawidłowy format danych"));
    }
}

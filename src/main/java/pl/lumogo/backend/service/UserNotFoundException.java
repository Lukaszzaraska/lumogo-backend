package pl.lumogo.backend.service;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Użytkownik nie znaleziony");
    }
}

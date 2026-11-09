package pl.lumogo.backend.service;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("Konto z tym adresem e-mail już istnieje");
    }
}

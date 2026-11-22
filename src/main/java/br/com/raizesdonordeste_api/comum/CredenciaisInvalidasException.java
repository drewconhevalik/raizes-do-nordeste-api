package br.com.raizesdonordeste_api.comum;

/** E-mail ou senha inválidos no login (HTTP 401). */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}

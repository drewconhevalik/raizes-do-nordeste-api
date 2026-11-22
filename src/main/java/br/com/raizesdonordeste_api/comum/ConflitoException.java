package br.com.raizesdonordeste_api.comum;

/** Conflito com o estado atual do recurso (HTTP 409). Ex.: transição de status inválida, e-mail já cadastrado. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}

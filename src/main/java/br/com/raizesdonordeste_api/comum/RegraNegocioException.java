package br.com.raizesdonordeste_api.comum;

/** Violação de regra de negócio (HTTP 422). Ex.: estoque insuficiente, desconto acima do limite. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}

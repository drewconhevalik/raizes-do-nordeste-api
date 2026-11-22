package br.com.raizesdonordeste_api.comum;

/** Dependência externa indisponível, como o serviço de pagamento (HTTP 503). */
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}

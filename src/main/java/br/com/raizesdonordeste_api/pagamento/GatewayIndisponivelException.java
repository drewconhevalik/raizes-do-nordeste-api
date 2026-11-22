package br.com.raizesdonordeste_api.pagamento;

/** Falha técnica do serviço externo de pagamento (indisponível, erro interno). */
public class GatewayIndisponivelException extends RuntimeException {
    public GatewayIndisponivelException(String mensagem) {
        super(mensagem);
    }
}

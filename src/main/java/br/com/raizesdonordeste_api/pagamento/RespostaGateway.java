package br.com.raizesdonordeste_api.pagamento;

public record RespostaGateway(ResultadoGateway resultado, String idTransacao, String mensagem) {

    public static RespostaGateway erro(String mensagem) {
        return new RespostaGateway(ResultadoGateway.ERRO, null, mensagem);
    }
}

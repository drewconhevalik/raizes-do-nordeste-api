package br.com.raizesdonordeste_api.pagamento;

/**
 * Porta de integração com o serviço externo de pagamento (seção 5 do estudo de caso).
 * As regras de negócio dependem apenas desta interface; trocar o provedor significa
 * criar outra implementação, sem alterar o restante do sistema.
 */
public interface GatewayPagamento {

    /** Solicita a cobrança. Pode lançar {@link GatewayIndisponivelException} em falha técnica. */
    RespostaGateway solicitarPagamento(SolicitacaoPagamento solicitacao);

    /** Solicita o estorno de uma transação aprovada. */
    void solicitarEstorno(String idTransacao);
}

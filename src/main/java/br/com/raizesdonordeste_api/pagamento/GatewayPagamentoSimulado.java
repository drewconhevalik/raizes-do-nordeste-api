package br.com.raizesdonordeste_api.pagamento;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Serviço de pagamento simulado, usado na demonstração e nos testes. O resultado é decidido
 * pelos centavos do valor, o que torna os cenários reproduzíveis:
 * <ul>
 * <li>,97 → recusado</li>
 * <li>,98 → falha técnica (serviço indisponível)</li>
 * <li>,99 → demora acima do tempo limite</li>
 * <li>demais valores → aprovado (cartão) ou pendente com confirmação posterior via webhook (Pix)</li>
 * </ul>
 */
@Component
public class GatewayPagamentoSimulado implements GatewayPagamento {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoSimulado.class);

    private final long atrasoMs;

    public GatewayPagamentoSimulado(@Value("${app.gateway.simulador.atraso-ms}") long atrasoMs) {
        this.atrasoMs = atrasoMs;
    }

    @Override
    public RespostaGateway solicitarPagamento(SolicitacaoPagamento solicitacao) {
        int centavos = solicitacao.valor().movePointRight(2).intValue() % 100;
        String idTransacao = "SIM-" + UUID.randomUUID();
        switch (centavos) {
            case 97:
                return new RespostaGateway(ResultadoGateway.RECUSADO, idTransacao, "Pagamento recusado pelo emissor.");
            case 98:
                throw new GatewayIndisponivelException("Serviço de pagamento indisponível (simulado).");
            case 99:
                esperar(atrasoMs);
                return new RespostaGateway(ResultadoGateway.APROVADO, idTransacao, "Aprovado após demora.");
            default:
                if (solicitacao.forma() == FormaPagamento.PIX) {
                    return new RespostaGateway(ResultadoGateway.PENDENTE, idTransacao,
                            "Aguardando confirmação do Pix (webhook).");
                }
                return new RespostaGateway(ResultadoGateway.APROVADO, idTransacao, "Pagamento aprovado.");
        }
    }

    @Override
    public void solicitarEstorno(String idTransacao) {
        log.info("Estorno solicitado ao serviço de pagamento simulado para a transação {}", idTransacao);
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

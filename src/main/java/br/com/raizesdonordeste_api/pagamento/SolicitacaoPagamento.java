package br.com.raizesdonordeste_api.pagamento;

import java.math.BigDecimal;

/** Dados enviados ao serviço externo. A chave de idempotência acompanha a solicitação. */
public record SolicitacaoPagamento(String chaveIdempotencia, BigDecimal valor, FormaPagamento forma) {
}

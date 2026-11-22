package br.com.raizesdonordeste_api.pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Integer id,
        Integer pedidoId,
        FormaPagamento forma,
        BigDecimal valor,
        StatusPagamento status,
        String idTransacaoExterna,
        LocalDateTime atualizadoEm) {

    public static PagamentoResponse de(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getForma(), p.getValor(), p.getStatus(),
                p.getIdTransacaoExterna(), p.getAtualizadoEm());
    }
}

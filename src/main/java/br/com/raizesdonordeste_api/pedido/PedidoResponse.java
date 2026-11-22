package br.com.raizesdonordeste_api.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Representação do pedido. Não expõe dados pessoais do cliente (LGPD). */
public record PedidoResponse(
        Integer id,
        String codigoAcompanhamento,
        Integer unidadeId,
        String unidadeNome,
        CanalPedido canal,
        TipoAtendimento tipoAtendimento,
        StatusPedido status,
        boolean clienteIdentificado,
        BigDecimal valorItens,
        BigDecimal valorDesconto,
        BigDecimal valorTotal,
        int pontosResgatados,
        LocalDateTime criadoEm,
        List<Item> itens,
        List<Historico> historico) {

    public record Item(Integer produtoId, String produto, int quantidade, BigDecimal precoUnitario,
            BigDecimal subtotal) {
    }

    public record Historico(StatusPedido statusAnterior, StatusPedido statusNovo, LocalDateTime dataHora) {
    }

    public static PedidoResponse de(Pedido p) {
        List<Item> itens = p.getItens().stream()
                .map(i -> new Item(i.getProduto().getId(), i.getProduto().getNome(), i.getQuantidade(),
                        i.getPrecoUnitario(), i.getSubtotal()))
                .toList();
        List<Historico> historico = p.getHistorico().stream()
                .map(h -> new Historico(h.getStatusAnterior(), h.getStatusNovo(), h.getDataHora()))
                .toList();
        return new PedidoResponse(p.getId(), p.getCodigoAcompanhamento(), p.getUnidade().getId(),
                p.getUnidade().getNome(), p.getCanal(), p.getTipoAtendimento(), p.getStatus(),
                p.getCliente() != null, p.getValorItens(), p.getValorDesconto(), p.getValorTotal(),
                p.getPontosResgatados(), p.getCriadoEm(), itens, historico);
    }
}

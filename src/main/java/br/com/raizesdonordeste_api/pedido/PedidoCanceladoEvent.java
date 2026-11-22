package br.com.raizesdonordeste_api.pedido;

/**
 * Evento publicado quando um pedido é cancelado. Os módulos de pagamento (estorno) e de
 * fidelidade (estorno de pontos) reagem a ele sem que o módulo de pedidos dependa deles,
 * o que mantém os módulos desacoplados.
 */
public record PedidoCanceladoEvent(Integer pedidoId, StatusPedido statusAnterior) {
}

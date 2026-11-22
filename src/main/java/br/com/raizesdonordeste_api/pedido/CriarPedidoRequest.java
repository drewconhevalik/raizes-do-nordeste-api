package br.com.raizesdonordeste_api.pedido;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Criação de pedido (RF08). O cliente é identificado pelo token, quando houver;
 * {@code pontosResgatar} é opcional (RF16, implementado na E7).
 */
public record CriarPedidoRequest(
        @NotNull Integer unidadeId,
        @NotNull CanalPedido canal,
        @NotNull TipoAtendimento tipoAtendimento,
        @NotEmpty @Size(max = 30) List<@Valid ItemPedidoRequest> itens,
        @PositiveOrZero Integer pontosResgatar) {
}

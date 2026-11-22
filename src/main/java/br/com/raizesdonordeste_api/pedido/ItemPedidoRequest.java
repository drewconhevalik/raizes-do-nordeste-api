package br.com.raizesdonordeste_api.pedido;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(
        @NotNull Integer produtoId,
        @NotNull @Positive @Max(50) Integer quantidade) {
}

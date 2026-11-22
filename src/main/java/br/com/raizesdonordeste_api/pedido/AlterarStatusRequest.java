package br.com.raizesdonordeste_api.pedido;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(@NotNull StatusPedido status) {
}

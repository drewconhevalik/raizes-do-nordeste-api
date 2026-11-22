package br.com.raizesdonordeste_api.cliente;

import jakarta.validation.constraints.NotNull;

public record ConsentimentoRequest(@NotNull Boolean concedido) {
}

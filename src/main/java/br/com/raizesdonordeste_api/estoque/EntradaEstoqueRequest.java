package br.com.raizesdonordeste_api.estoque;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EntradaEstoqueRequest(
        @NotNull @Positive Integer quantidade,
        @Size(max = 255) String motivo) {
}

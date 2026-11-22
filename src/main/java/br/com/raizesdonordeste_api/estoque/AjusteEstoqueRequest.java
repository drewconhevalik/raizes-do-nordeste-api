package br.com.raizesdonordeste_api.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Ajuste manual (ex.: inventário, perda). O motivo é obrigatório porque a operação é auditada. */
public record AjusteEstoqueRequest(
        @NotNull @PositiveOrZero Integer novaQuantidade,
        @NotBlank @Size(max = 255) String motivo) {
}

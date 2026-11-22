package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/** Configuração de um produto no cardápio da unidade. Preço local nulo = preço base do catálogo. */
public record CardapioItemRequest(
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoLocal,
        boolean disponivel,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        @Size(max = 255) String observacaoRegional) {
}

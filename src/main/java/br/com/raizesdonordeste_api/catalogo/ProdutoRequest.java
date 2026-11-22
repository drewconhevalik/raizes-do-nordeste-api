package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 255) String descricao,
        @NotNull CategoriaProduto categoria,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoBase,
        boolean exigeCozinhaCompleta,
        Boolean ativo) {
}

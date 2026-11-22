package br.com.raizesdonordeste_api.pedido;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DescontoRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal valor,
        @NotBlank @Size(max = 255) String motivo) {
}

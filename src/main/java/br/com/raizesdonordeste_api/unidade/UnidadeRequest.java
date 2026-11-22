package br.com.raizesdonordeste_api.unidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "deve ter 2 letras maiúsculas") String uf,
        @NotNull Regiao regiao,
        @NotNull FormatoUnidade formato) {
}

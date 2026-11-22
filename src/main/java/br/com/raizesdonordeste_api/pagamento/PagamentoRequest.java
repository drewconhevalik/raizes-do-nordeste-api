package br.com.raizesdonordeste_api.pagamento;

import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(@NotNull FormaPagamento forma) {
}

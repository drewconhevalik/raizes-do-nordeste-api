package br.com.raizesdonordeste_api.pagamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Notificação assíncrona enviada pelo serviço de pagamento (ex.: confirmação de Pix). */
public record WebhookRequest(
        @NotBlank String idTransacao,
        @NotNull ResultadoGateway status) {
}

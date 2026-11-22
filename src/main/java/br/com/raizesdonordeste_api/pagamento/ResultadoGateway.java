package br.com.raizesdonordeste_api.pagamento;

/** Resposta possível do serviço externo de pagamento. ERRO representa falha técnica ou tempo esgotado. */
public enum ResultadoGateway {
    APROVADO, RECUSADO, PENDENTE, ERRO
}

package br.com.raizesdonordeste_api.seguranca;

public record TokenResponse(
        String token,
        String tipo,
        long expiraEmSegundos,
        String nome,
        String papel,
        Integer unidadeId) {
}

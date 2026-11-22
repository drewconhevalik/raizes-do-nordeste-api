package br.com.raizesdonordeste_api.cliente;

import java.time.LocalDate;

/** Dados do próprio titular (direito de acesso). Nunca devolvido para perfis da equipe. */
public record ClienteResponse(
        Integer id,
        String nome,
        String email,
        String telefone,
        LocalDate dataNascimento,
        String faixaEtaria,
        int saldoPontos) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone(), c.getDataNascimento(),
                c.faixaEtaria(), c.getSaldoPontos());
    }
}

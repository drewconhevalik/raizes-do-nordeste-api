package br.com.raizesdonordeste_api.cliente;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Cadastro no programa de fidelidade (RF02). Os consentimentos são separados por finalidade
 * e nenhum vem marcado por padrão (LGPD: consentimento livre e explícito).
 */
public record CadastroClienteRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @Size(max = 20) String telefone,
        @Past LocalDate dataNascimento,
        @NotNull @AssertTrue(message = "é obrigatório declarar ter 18 anos ou mais") Boolean declaraMaioridade,
        boolean consentimentoFidelidade,
        boolean consentimentoMarketing) {
}

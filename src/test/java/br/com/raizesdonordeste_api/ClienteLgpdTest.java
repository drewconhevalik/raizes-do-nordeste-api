package br.com.raizesdonordeste_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.RegistroAuditoriaRepository;
import br.com.raizesdonordeste_api.cliente.ClienteRepository;

/** Cadastro, consentimento e direitos do titular (RF02, RF19, RN10–RN12, RN18). */
@SpringBootTest
class ClienteLgpdTest extends ApiTestBase {

    @Autowired
    private RegistroAuditoriaRepository auditoriaRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    private static String emailNovo() {
        return "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    private static String cadastroJson(String email, String maioridade, String nascimento, boolean fidelidade) {
        return "{\"nome\":\"Cliente Teste\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\","
                + (nascimento != null ? "\"dataNascimento\":\"" + nascimento + "\"," : "")
                + (maioridade != null ? "\"declaraMaioridade\":" + maioridade + "," : "")
                + "\"consentimentoFidelidade\":" + fidelidade + ",\"consentimentoMarketing\":false}";
    }

    private void cadastrar(String email, boolean fidelidade) throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(email, "true", null, fidelidade)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CT-CLI-01: cadastro sem declaração de maioridade é rejeitado (400)")
    void semMaioridade() throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(emailNovo(), null, null, true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-CLI-02: data de nascimento de menor de 18 anos é rejeitada (422)")
    void menorDeIdade() throws Exception {
        String nascimento = java.time.LocalDate.now().minusYears(15).toString();
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(emailNovo(), "true", nascimento, true)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("CT-CLI-03: cadastro válido (201) e e-mail repetido (409); senha não é devolvida")
    void cadastroEDuplicidade() throws Exception {
        String email = emailNovo();
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(email, "true", "1995-03-20", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.faixaEtaria").exists())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(email, "true", null, true)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CT-CLI-04: revogação do consentimento é auditada e interrompe o acúmulo de pontos")
    void revogacao() throws Exception {
        String email = emailNovo();
        cadastrar(email, true);
        String token = tokenCliente(email);
        long antes = auditoriaRepository.countByAcao(AcaoAuditoria.REVOGACAO_CONSENTIMENTO);

        mvc.perform(put("/api/v1/clientes/me/consentimentos/FIDELIDADE").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"concedido\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoAtual.FIDELIDADE").value(false))
                .andExpect(jsonPath("$.historico.length()").value(3));
        assertThat(auditoriaRepository.countByAcao(AcaoAuditoria.REVOGACAO_CONSENTIMENTO)).isEqualTo(antes + 1);

        Integer pedido = ler(criarPedido(token, "APP", "Café coado", 2), "$.id");
        pagar(pedido, "CARTAO_CREDITO", UUID.randomUUID().toString(), token).andExpect(status().isCreated());
        assertThat(clienteRepository.findByEmail(email).orElseThrow().getSaldoPontos()).isZero();
    }

    @Test
    @DisplayName("CT-CLI-05: titular acessa os próprios dados (acesso auditado); equipe não acessa (403)")
    void acessoAosDados() throws Exception {
        String email = emailNovo();
        cadastrar(email, false);
        long antes = auditoriaRepository.countByAcao(AcaoAuditoria.ACESSO_DADOS_PESSOAIS);
        mvc.perform(get("/api/v1/clientes/me").header("Authorization", tokenCliente(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        assertThat(auditoriaRepository.countByAcao(AcaoAuditoria.ACESSO_DADOS_PESSOAIS)).isEqualTo(antes + 1);

        mvc.perform(get("/api/v1/clientes/me").header("Authorization", tokenEquipe("gerente.recife@rnordeste.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-CLI-06: eliminação anonimiza os dados (204) e impede novo login (401)")
    void eliminacao() throws Exception {
        String email = emailNovo();
        cadastrar(email, true);
        long antes = auditoriaRepository.countByAcao(AcaoAuditoria.ANONIMIZACAO_CLIENTE);
        mvc.perform(delete("/api/v1/clientes/me").header("Authorization", tokenCliente(email)))
                .andExpect(status().isNoContent());
        assertThat(clienteRepository.findByEmail(email)).isEmpty();
        assertThat(auditoriaRepository.countByAcao(AcaoAuditoria.ANONIMIZACAO_CLIENTE)).isEqualTo(antes + 1);

        mvc.perform(post("/api/v1/auth/clientes/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}

package br.com.raizesdonordeste_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.RegistroAuditoriaRepository;

/** Cardápio por unidade (RN01, RN02) e estoque (RF07). */
@SpringBootTest
class CardapioEstoqueTest extends ApiTestBase {

    @Autowired
    private RegistroAuditoriaRepository auditoriaRepository;

    @Test
    @DisplayName("CT-CAR-01: produto fora da vigência sazonal não aparece no cardápio")
    void sazonalForaDaVigencia() throws Exception {
        String corpo = mvc.perform(get("/api/v1/unidades/" + unidadeId("Raízes Boa Viagem") + "/cardapio"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> nomes = ler(corpo, "$[*].nome");
        assertThat(nomes).contains("Tapioca de carne de sol").doesNotContain("Canjica");
    }

    @Test
    @DisplayName("CT-CAR-02: unidade reduzida não oferece produto que exige cozinha completa e usa preço local")
    void unidadeReduzidaEPrecoLocal() throws Exception {
        String corpo = mvc.perform(get("/api/v1/unidades/" + unidadeId("Raízes Paulista") + "/cardapio"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> nomes = ler(corpo, "$[*].nome");
        assertThat(nomes).doesNotContain("Cuscuz recheado com charque", "Café da manhã completo");
        List<Double> precos = ler(corpo, "$[?(@.nome == 'Tapioca de carne de sol')].preco");
        assertThat(precos).containsExactly(21.90);
    }

    @Test
    @DisplayName("CT-CAR-03: gerente de outra unidade não altera o cardápio (403)")
    void gerenteDeOutraUnidade() throws Exception {
        String token = tokenEquipe("gerente.sp@rnordeste.com");
        mvc.perform(put("/api/v1/unidades/" + unidadeId("Raízes Boa Viagem") + "/cardapio/"
                        + produtoId("Café coado"))
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precoLocal\":6.00,\"disponivel\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-CAR-04: alteração de preço local pelo gerente é auditada")
    void alteracaoPrecoAuditada() throws Exception {
        long antes = auditoriaRepository.countByAcao(AcaoAuditoria.ALTERACAO_PRECO);
        String token = tokenEquipe("gerente.recife@rnordeste.com");
        mvc.perform(put("/api/v1/unidades/" + unidadeId("Raízes Boa Viagem") + "/cardapio/"
                        + produtoId("Café coado"))
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precoLocal\":5.50,\"disponivel\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(5.50));
        assertThat(auditoriaRepository.countByAcao(AcaoAuditoria.ALTERACAO_PRECO)).isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("CT-EST-01: ajuste de estoque sem motivo é rejeitado (400)")
    void ajusteSemMotivo() throws Exception {
        String token = tokenEquipe("gerente.recife@rnordeste.com");
        mvc.perform(post("/api/v1/unidades/" + unidadeId("Raízes Boa Viagem") + "/estoque/"
                        + produtoId("Suco de graviola") + "/ajustes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaQuantidade\":45}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-EST-02: ajuste com motivo altera o saldo e gera auditoria")
    void ajusteComMotivo() throws Exception {
        long antes = auditoriaRepository.countByAcao(AcaoAuditoria.AJUSTE_ESTOQUE);
        String token = tokenEquipe("gerente.recife@rnordeste.com");
        Integer recife = unidadeId("Raízes Boa Viagem");
        Integer suco = produtoId("Suco de graviola");
        mvc.perform(post("/api/v1/unidades/" + recife + "/estoque/" + suco + "/ajustes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaQuantidade\":45,\"motivo\":\"Inventário semanal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(45));
        assertThat(saldo(recife, suco)).isEqualTo(45);
        assertThat(auditoriaRepository.countByAcao(AcaoAuditoria.AJUSTE_ESTOQUE)).isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("CT-EST-03: atendente não consulta estoque (403)")
    void atendenteSemAcessoAoEstoque() throws Exception {
        String token = tokenEquipe("atendente.recife@rnordeste.com");
        mvc.perform(get("/api/v1/unidades/" + unidadeId("Raízes Boa Viagem") + "/estoque")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }
}

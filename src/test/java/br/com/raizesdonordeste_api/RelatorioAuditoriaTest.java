package br.com.raizesdonordeste_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/** Relatórios da matriz (RF22, RF23) e consulta da trilha de auditoria (RF21). */
@SpringBootTest
class RelatorioAuditoriaTest extends ApiTestBase {

    /** Garante ao menos uma venda paga e um cancelamento auditado antes de cada consulta. */
    @BeforeEach
    void prepararMovimento() throws Exception {
        Integer pago = ler(criarPedido(null, "TOTEM", "Tapioca de carne de sol", 2), "$.id");
        pagar(pago, "CARTAO_CREDITO", UUID.randomUUID().toString(), null).andExpect(status().isCreated());

        Integer cancelado = ler(criarPedido(null, "TOTEM", "Café coado", 1), "$.id");
        mvc.perform(post("/api/v1/pedidos/" + cancelado + "/cancelamento")
                        .header("Authorization", tokenEquipe("atendente.recife@rnordeste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Teste de auditoria\"}"))
                .andExpect(status().isOk());
    }

    private String consultar(String caminho, String token) throws Exception {
        return mvc.perform(get(caminho).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("CT-REL-01: matriz consulta vendas por unidade")
    void vendasPorUnidade() throws Exception {
        String corpo = consultar("/api/v1/relatorios/vendas?agrupamento=UNIDADE", tokenEquipe("admin@rnordeste.com"));
        List<String> grupos = ler(corpo, "$.linhas[*].grupo");
        assertThat(grupos).contains("Raízes Boa Viagem");
        assertThat(this.<Integer>ler(corpo, "$.quantidadeTotal")).isPositive();
    }

    @Test
    @DisplayName("CT-REL-02: matriz consulta vendas por região")
    void vendasPorRegiao() throws Exception {
        String corpo = consultar("/api/v1/relatorios/vendas?agrupamento=REGIAO", tokenEquipe("admin@rnordeste.com"));
        List<String> grupos = ler(corpo, "$.linhas[*].grupo");
        assertThat(grupos).contains("NORDESTE");
    }

    @Test
    @DisplayName("CT-REL-03: gerente só vê a própria unidade; pedir outra unidade devolve 403")
    void gerenteRestritoAUnidade() throws Exception {
        String token = tokenEquipe("gerente.sp@rnordeste.com");
        mvc.perform(get("/api/v1/relatorios/vendas?unidadeId=" + unidadeId("Raízes Boa Viagem"))
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
        String corpo = consultar("/api/v1/relatorios/vendas", token);
        List<String> grupos = ler(corpo, "$.linhas[*].grupo");
        assertThat(grupos).doesNotContain("Raízes Boa Viagem");
    }

    @Test
    @DisplayName("CT-REL-04: ranking de produtos mais vendidos")
    void maisVendidos() throws Exception {
        String corpo = consultar("/api/v1/relatorios/produtos-mais-vendidos?limite=5",
                tokenEquipe("admin@rnordeste.com"));
        List<String> produtos = ler(corpo, "$.linhas[*].grupo");
        assertThat(produtos).isNotEmpty().contains("Tapioca de carne de sol");
    }

    @Test
    @DisplayName("CT-AUD-01: matriz consulta a auditoria filtrando por ação; atendente não acessa (403)")
    void consultaAuditoria() throws Exception {
        String corpo = consultar("/api/v1/auditoria?acao=CANCELAMENTO_PEDIDO", tokenEquipe("admin@rnordeste.com"));
        List<String> acoes = ler(corpo, "$[*].acao");
        assertThat(acoes).isNotEmpty().allMatch("CANCELAMENTO_PEDIDO"::equals);

        mvc.perform(get("/api/v1/auditoria").header("Authorization", tokenEquipe("atendente.recife@rnordeste.com")))
                .andExpect(status().isForbidden());
    }
}

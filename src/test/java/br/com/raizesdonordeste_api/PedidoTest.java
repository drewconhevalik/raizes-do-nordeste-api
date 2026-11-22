package br.com.raizesdonordeste_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.RegistroAuditoriaRepository;

/** Pedidos: criação multicanal, estoque, ciclo de status, cancelamento e desconto. */
@SpringBootTest
class PedidoTest extends ApiTestBase {

    @Autowired
    private RegistroAuditoriaRepository auditoriaRepository;

    private String criarPedidoTotem(String produto, int quantidade) throws Exception {
        return mvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(unidadeId("Raízes Boa Viagem"), "TOTEM", produtoId(produto), quantidade)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("CT-PED-01: totem anônimo cria pedido, calcula total e reserva estoque")
    void totemCriaPedido() throws Exception {
        Integer recife = unidadeId("Raízes Boa Viagem");
        Integer tapioca = produtoId("Tapioca de queijo coalho");
        int saldoAntes = saldo(recife, tapioca);

        String corpo = criarPedidoTotem("Tapioca de queijo coalho", 2);

        assertThat(this.<String>ler(corpo, "$.status")).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(this.<Double>ler(corpo, "$.valorTotal")).isEqualTo(29.00);
        assertThat(this.<Boolean>ler(corpo, "$.clienteIdentificado")).isFalse();
        assertThat(this.<String>ler(corpo, "$.codigoAcompanhamento")).isNotBlank();
        assertThat(saldo(recife, tapioca)).isEqualTo(saldoAntes - 2);
    }

    @Test
    @DisplayName("CT-PED-02: estoque insuficiente devolve 422 e nada é gravado")
    void estoqueInsuficiente() throws Exception {
        Integer recife = unidadeId("Raízes Boa Viagem");
        Integer bolo = produtoId("Bolo de macaxeira");
        int saldoAntes = saldo(recife, bolo);
        mvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(recife, "TOTEM", bolo, saldoAntes + 1)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
        assertThat(saldo(recife, bolo)).isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("CT-PED-03: produto fora da vigência não pode ser pedido (422)")
    void produtoIndisponivel() throws Exception {
        mvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(unidadeId("Raízes Boa Viagem"), "TOTEM", produtoId("Canjica"), 1)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("CT-PED-04: pedido pelo APP sem cliente autenticado é rejeitado (422)")
    void appSemCliente() throws Exception {
        mvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(unidadeId("Raízes Boa Viagem"), "APP", produtoId("Café coado"), 1)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("CT-PED-05: cliente autenticado cria pedido pelo APP e consulta o próprio pedido")
    void clienteApp() throws Exception {
        String token = tokenCliente("maria@email.com");
        String corpo = mvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(unidadeId("Raízes Boa Viagem"), "APP", produtoId("Café coado"), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteIdentificado").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer id = ler(corpo, "$.id");
        mvc.perform(get("/api/v1/pedidos/" + id).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CT-PED-06: dados inválidos (lista de itens vazia) devolvem 400")
    void itensVazios() throws Exception {
        mvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidadeId\":1,\"canal\":\"TOTEM\",\"tipoAtendimento\":\"RETIRADA\",\"itens\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-PED-07: transição de status inválida devolve 409")
    void transicaoInvalida() throws Exception {
        Integer id = ler(criarPedidoTotem("Café coado", 1), "$.id");
        String token = tokenEquipe("cozinha.recife@rnordeste.com");
        mvc.perform(patch("/api/v1/pedidos/" + id + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PRONTO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CT-PED-08: status PAGO não pode ser definido manualmente (422)")
    void pagoManual() throws Exception {
        Integer id = ler(criarPedidoTotem("Café coado", 1), "$.id");
        String token = tokenEquipe("gerente.recife@rnordeste.com");
        mvc.perform(patch("/api/v1/pedidos/" + id + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAGO\"}"))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("CT-PED-09: atendente cancela pedido não pago; estoque volta e cancelamento é auditado")
    void cancelamento() throws Exception {
        Integer recife = unidadeId("Raízes Boa Viagem");
        Integer cafe = produtoId("Café coado");
        int saldoAntes = saldo(recife, cafe);
        Integer id = ler(criarPedidoTotem("Café coado", 3), "$.id");
        assertThat(saldo(recife, cafe)).isEqualTo(saldoAntes - 3);

        String token = tokenEquipe("atendente.recife@rnordeste.com");
        mvc.perform(post("/api/v1/pedidos/" + id + "/cancelamento")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cliente desistiu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertThat(saldo(recife, cafe)).isEqualTo(saldoAntes);
        assertThat(auditoriaRepository.countByAcaoAndEntidadeId(AcaoAuditoria.CANCELAMENTO_PEDIDO, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("CT-PED-10: cancelamento sem motivo é rejeitado (400)")
    void cancelamentoSemMotivo() throws Exception {
        Integer id = ler(criarPedidoTotem("Café coado", 1), "$.id");
        String token = tokenEquipe("atendente.recife@rnordeste.com");
        mvc.perform(post("/api/v1/pedidos/" + id + "/cancelamento")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-PED-11: desconto acima de 20% é rejeitado; desconto válido reduz o total e é auditado")
    void desconto() throws Exception {
        Integer id = ler(criarPedidoTotem("Tapioca de carne de sol", 1), "$.id"); // R$ 18,90 → limite R$ 3,78
        String token = tokenEquipe("gerente.recife@rnordeste.com");

        mvc.perform(post("/api/v1/pedidos/" + id + "/desconto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":5.00,\"motivo\":\"Cortesia\"}"))
                .andExpect(status().is(422));

        mvc.perform(post("/api/v1/pedidos/" + id + "/desconto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":2.00,\"motivo\":\"Atraso no atendimento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(16.90));

        assertThat(auditoriaRepository.countByAcaoAndEntidadeId(AcaoAuditoria.DESCONTO_PEDIDO, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("CT-PED-12: atendente não pode aplicar desconto (403)")
    void atendenteSemDesconto() throws Exception {
        Integer id = ler(criarPedidoTotem("Café coado", 1), "$.id");
        String token = tokenEquipe("atendente.recife@rnordeste.com");
        mvc.perform(post("/api/v1/pedidos/" + id + "/desconto")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":0.50,\"motivo\":\"Teste\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-PED-13: gerente de outra unidade não vê a fila de Recife (403)")
    void filaDeOutraUnidade() throws Exception {
        String token = tokenEquipe("gerente.sp@rnordeste.com");
        mvc.perform(get("/api/v1/unidades/" + unidadeId("Raízes Boa Viagem") + "/pedidos")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-PED-14: acompanhamento público pelo código do pedido")
    void acompanhamentoPublico() throws Exception {
        String codigo = ler(criarPedidoTotem("Café coado", 1), "$.codigoAcompanhamento");
        mvc.perform(get("/api/v1/pedidos/acompanhamento/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
    }
}

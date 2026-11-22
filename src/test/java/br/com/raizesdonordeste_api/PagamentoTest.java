package br.com.raizesdonordeste_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import br.com.raizesdonordeste_api.cliente.ClienteRepository;
import br.com.raizesdonordeste_api.pedido.PedidoService;

/** Pagamento simulado, idempotência, webhook, estorno, expiração e pontos (RF11–RF16, RN04, RN08, RN14). */
@SpringBootTest
class PagamentoTest extends ApiTestBase {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private PedidoService pedidoService;

    private static String chave() {
        return UUID.randomUUID().toString();
    }

    private int saldoMaria() {
        return clienteRepository.findByEmail("maria@email.com").orElseThrow().getSaldoPontos();
    }

    private String statusDoPedido(String json) throws Exception {
        String codigo = ler(json, "$.codigoAcompanhamento");
        String corpo = mvc.perform(get("/api/v1/pedidos/acompanhamento/" + codigo))
                .andReturn().getResponse().getContentAsString();
        return ler(corpo, "$.status");
    }

    @Test
    @DisplayName("CT-PAG-01: cartão aprovado muda o pedido para PAGO e credita 1 ponto por real")
    void cartaoAprovado() throws Exception {
        String token = tokenCliente("maria@email.com");
        int saldoAntes = saldoMaria();
        String pedido = criarPedido(token, "APP", "Tapioca de carne de sol", 1);
        Integer id = ler(pedido, "$.id");
        double total = this.<Double>ler(pedido, "$.valorTotal");

        pagar(id, "CARTAO_CREDITO", chave(), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"));

        assertThat(statusDoPedido(pedido)).isEqualTo("PAGO");
        assertThat(saldoMaria()).isEqualTo(saldoAntes + (int) Math.floor(total));
    }

    @Test
    @DisplayName("CT-PAG-02: mesma Idempotency-Key repetida não gera nova cobrança (200, mesmo pagamento)")
    void idempotencia() throws Exception {
        Integer id = ler(criarPedido(null, "TOTEM", "Café coado", 1), "$.id");
        String chave = chave();
        String primeiro = pagar(id, "CARTAO_DEBITO", chave, null).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String segundo = pagar(id, "CARTAO_DEBITO", chave, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(this.<Integer>ler(segundo, "$.id")).isEqualTo(ler(primeiro, "$.id"));

        mvc.perform(get("/api/v1/pedidos/" + id + "/pagamentos")
                        .header("Authorization", tokenEquipe("gerente.recife@rnordeste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("CT-PAG-03: pagamento recusado (valor terminado em ,97) mantém o pedido aguardando")
    void recusado() throws Exception {
        String pedido = criarPedido(null, "TOTEM", "Água de coco", 1);
        pagar(ler(pedido, "$.id"), "CARTAO_CREDITO", chave(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"));
        assertThat(statusDoPedido(pedido)).isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    @DisplayName("CT-PAG-04: serviço de pagamento indisponível (,98) devolve 503 e preserva o pedido")
    void servicoIndisponivel() throws Exception {
        String pedido = criarPedido(null, "TOTEM", "Cocada", 1);
        Integer id = ler(pedido, "$.id");
        pagar(id, "CARTAO_CREDITO", chave(), null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Serviço indisponível"));
        assertThat(statusDoPedido(pedido)).isEqualTo("AGUARDANDO_PAGAMENTO");
        mvc.perform(get("/api/v1/pedidos/" + id + "/pagamentos")
                        .header("Authorization", tokenEquipe("gerente.recife@rnordeste.com")))
                .andExpect(jsonPath("$[0].status").value("ERRO"));
    }

    @Test
    @DisplayName("CT-PAG-05: demora acima do tempo limite (,99) devolve 503 sem travar a API")
    void tempoLimite() throws Exception {
        String pedido = criarPedido(null, "TOTEM", "Queijadinha", 1);
        long inicio = System.currentTimeMillis();
        pagar(ler(pedido, "$.id"), "CARTAO_CREDITO", chave(), null).andExpect(status().isServiceUnavailable());
        assertThat(System.currentTimeMillis() - inicio).isLessThan(2900);
        assertThat(statusDoPedido(pedido)).isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    @DisplayName("CT-PAG-06: Pix fica pendente e é confirmado pelo webhook; webhook repetido não altera nada")
    void pixComWebhook() throws Exception {
        String pedido = criarPedido(null, "TOTEM", "Café coado", 1);
        Integer id = ler(pedido, "$.id");
        String pagamento = pagar(id, "PIX", chave(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();
        String transacao = ler(pagamento, "$.idTransacaoExterna");

        // nova tentativa enquanto há Pix pendente
        pagar(id, "CARTAO_CREDITO", chave(), null).andExpect(status().isConflict());

        String webhook = "{\"idTransacao\":\"" + transacao + "\",\"status\":\"APROVADO\"}";
        mvc.perform(post("/api/v1/pagamentos/webhook").contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/pagamentos/webhook").header("X-Gateway-Token", "dev-token-webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
        assertThat(statusDoPedido(pedido)).isEqualTo("PAGO");

        mvc.perform(post("/api/v1/pagamentos/webhook").header("X-Gateway-Token", "dev-token-webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    @DisplayName("CT-PAG-07: pedido já pago não aceita novo pagamento (409)")
    void pedidoJaPago() throws Exception {
        Integer id = ler(criarPedido(null, "TOTEM", "Café coado", 1), "$.id");
        pagar(id, "CARTAO_DEBITO", chave(), null).andExpect(status().isCreated());
        pagar(id, "CARTAO_DEBITO", chave(), null).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CT-PAG-08: sem Idempotency-Key a solicitação é rejeitada (400)")
    void semChave() throws Exception {
        Integer id = ler(criarPedido(null, "TOTEM", "Café coado", 1), "$.id");
        mvc.perform(post("/api/v1/pedidos/" + id + "/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"forma\":\"PIX\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-PAG-09: gerente cancela pedido pago; estorno solicitado e pontos retirados; atendente não pode (403)")
    void cancelamentoDePedidoPago() throws Exception {
        String tokenMaria = tokenCliente("maria@email.com");
        int saldoAntes = saldoMaria();
        Integer id = ler(criarPedido(tokenMaria, "APP", "Tapioca de queijo coalho", 1), "$.id");
        pagar(id, "CARTAO_CREDITO", chave(), tokenMaria).andExpect(status().isCreated());
        assertThat(saldoMaria()).isGreaterThan(saldoAntes);

        String corpo = "{\"motivo\":\"Produto em falta na cozinha\"}";
        mvc.perform(post("/api/v1/pedidos/" + id + "/cancelamento")
                        .header("Authorization", tokenEquipe("atendente.recife@rnordeste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/" + id + "/cancelamento")
                        .header("Authorization", tokenEquipe("gerente.recife@rnordeste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        mvc.perform(get("/api/v1/pedidos/" + id + "/pagamentos").header("Authorization", tokenMaria))
                .andExpect(jsonPath("$[0].status").value("ESTORNO_SOLICITADO"));
        assertThat(saldoMaria()).isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("CT-PAG-10: pedido sem pagamento expira em 15 minutos e devolve o estoque (RN04)")
    void expiracao() throws Exception {
        Integer recife = unidadeId("Raízes Boa Viagem");
        Integer suco = produtoId("Suco de graviola");
        int saldoAntes = saldo(recife, suco);
        String pedido = criarPedido(null, "TOTEM", "Suco de graviola", 2);
        assertThat(saldo(recife, suco)).isEqualTo(saldoAntes - 2);

        // simula a passagem do tempo: considera vencido tudo o que foi criado até daqui a 1 minuto
        int expirados = pedidoService.expirarPendentes(LocalDateTime.now().plusMinutes(1));

        assertThat(expirados).isGreaterThanOrEqualTo(1);
        assertThat(statusDoPedido(pedido)).isEqualTo("CANCELADO");
        assertThat(saldo(recife, suco)).isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("CT-PAG-11: resgate de 100 pontos dá R$ 5,00 de desconto; resgate fora de bloco é rejeitado (422)")
    void resgateDePontos() throws Exception {
        String token = tokenCliente("maria@email.com");
        int saldoAntes = saldoMaria();
        String base = "{\"unidadeId\":" + unidadeId("Raízes Boa Viagem") + ",\"canal\":\"APP\","
                + "\"tipoAtendimento\":\"RETIRADA\",\"itens\":[{\"produtoId\":" + produtoId("Tapioca de carne de sol")
                + ",\"quantidade\":1}],\"pontosResgatar\":";

        mvc.perform(post("/api/v1/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(base + "150}"))
                .andExpect(status().is(422));

        mvc.perform(post("/api/v1/pedidos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(base + "100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorDesconto").value(5.00))
                .andExpect(jsonPath("$.valorTotal").value(13.90))
                .andExpect(jsonPath("$.pontosResgatados").value(100));
        assertThat(saldoMaria()).isEqualTo(saldoAntes - 100);
    }
}

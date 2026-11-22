package br.com.raizesdonordeste_api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import br.com.raizesdonordeste_api.catalogo.Produto;
import br.com.raizesdonordeste_api.catalogo.ProdutoRepository;
import br.com.raizesdonordeste_api.estoque.EstoqueRepository;
import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.unidade.UnidadeRepository;

/** Apoio comum aos testes de API: MockMvc com segurança, login e consultas aos dados de demonstração. */
public abstract class ApiTestBase {

    protected static final String SENHA = "Senha@123";

    @Autowired
    protected WebApplicationContext contexto;
    @Autowired
    protected ProdutoRepository produtoRepository;
    @Autowired
    protected UnidadeRepository unidadeRepository;
    @Autowired
    protected EstoqueRepository estoqueRepository;

    protected MockMvc mvc;

    @BeforeEach
    void configurarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    protected String tokenEquipe(String email) throws Exception {
        return login("/api/v1/auth/login", email);
    }

    protected String tokenCliente(String email) throws Exception {
        return login("/api/v1/auth/clientes/login", email);
    }

    private String login(String caminho, String email) throws Exception {
        String corpo = mvc.perform(post(caminho)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(corpo, "$.token");
    }

    protected Integer produtoId(String nome) {
        return produtoRepository.findAll().stream().filter(p -> p.getNome().equals(nome)).map(Produto::getId)
                .findFirst().orElseThrow();
    }

    protected Integer unidadeId(String nome) {
        return unidadeRepository.findAll().stream().filter(u -> u.getNome().equals(nome)).map(Unidade::getId)
                .findFirst().orElseThrow();
    }

    protected int saldo(Integer unidadeId, Integer produtoId) {
        return estoqueRepository.buscar(unidadeId, produtoId).orElseThrow().getQuantidade();
    }

    protected static String pedidoJson(Integer unidadeId, String canal, Integer produtoId, int quantidade) {
        return "{\"unidadeId\":" + unidadeId + ",\"canal\":\"" + canal + "\",\"tipoAtendimento\":\"RETIRADA\","
                + "\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":" + quantidade + "}]}";
    }

    /** Cria pedido na unidade de Recife; token nulo = pedido anônimo. Devolve o JSON da resposta. */
    protected String criarPedido(String token, String canal, String produto, int quantidade) throws Exception {
        MockHttpServletRequestBuilder req = post("/api/v1/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson(unidadeId("Raízes Boa Viagem"), canal, produtoId(produto), quantidade));
        if (token != null) {
            req.header("Authorization", token);
        }
        return mvc.perform(req).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    protected ResultActions pagar(Integer pedidoId, String forma, String chave, String token) throws Exception {
        MockHttpServletRequestBuilder req = post("/api/v1/pedidos/" + pedidoId + "/pagamentos")
                .header("Idempotency-Key", chave)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"forma\":\"" + forma + "\"}");
        if (token != null) {
            req.header("Authorization", token);
        }
        return mvc.perform(req);
    }

    protected <T> T ler(String json, String caminho) {
        return JsonPath.read(json, caminho);
    }
}

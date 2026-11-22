package br.com.raizesdonordeste_api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Testes de autenticação e autorização por perfil (RF01, RNF01). */
@SpringBootTest
class AutenticacaoTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String login(String caminho, String email, String senha) throws Exception {
        String corpo = mvc.perform(post(caminho)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(corpo);
        if (!m.find()) {
            throw new IllegalStateException("Token não encontrado na resposta: " + corpo);
        }
        return m.group(1);
    }

    private static final String UNIDADE_JSON =
            "{\"nome\":\"Raízes Teste\",\"cidade\":\"Natal\",\"uf\":\"RN\",\"regiao\":\"NORDESTE\",\"formato\":\"REDUZIDA\"}";

    @Test
    @DisplayName("CT-AUT-01: login válido de usuário interno devolve token e perfil")
    void loginValido() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gerente.recife@rnordeste.com\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.papel").value("GERENTE"))
                .andExpect(jsonPath("$.unidadeId").isNumber());
    }

    @Test
    @DisplayName("CT-AUT-02: senha incorreta devolve 401 sem token")
    void loginSenhaErrada() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gerente.recife@rnordeste.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("CT-AUT-03: e-mail em formato inválido devolve 400")
    void loginEmailInvalido() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-e-email\",\"senha\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CT-AUT-04: login de cliente devolve papel CLIENTE")
    void loginCliente() throws Exception {
        mvc.perform(post("/api/v1/auth/clientes/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("CLIENTE"));
    }

    @Test
    @DisplayName("CT-AUT-05: rota protegida sem token devolve 401")
    void semToken() throws Exception {
        mvc.perform(post("/api/v1/unidades").contentType(MediaType.APPLICATION_JSON).content(UNIDADE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CT-AUT-06: token adulterado devolve 401")
    void tokenInvalido() throws Exception {
        mvc.perform(post("/api/v1/unidades")
                        .header("Authorization", "Bearer token.invalido.123")
                        .contentType(MediaType.APPLICATION_JSON).content(UNIDADE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CT-AUT-07: perfil sem permissão (atendente criando unidade) devolve 403")
    void perfilSemPermissao() throws Exception {
        String token = login("/api/v1/auth/login", "atendente.recife@rnordeste.com", "Senha@123");
        mvc.perform(post("/api/v1/unidades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(UNIDADE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-AUT-08: administrador da matriz cria unidade (201)")
    void adminCriaUnidade() throws Exception {
        String token = login("/api/v1/auth/login", "admin@rnordeste.com", "Senha@123");
        mvc.perform(post("/api/v1/unidades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(UNIDADE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.uf").value("RN"));
    }

    @Test
    @DisplayName("CT-AUT-09: rotas públicas (unidades e health check) não exigem token")
    void rotasPublicas() throws Exception {
        mvc.perform(get("/api/v1/unidades")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

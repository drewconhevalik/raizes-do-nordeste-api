package br.com.raizesdonordeste_api.seguranca;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Dados de quem está fazendo a requisição, lidos do token JWT.
 *
 * @param id        identificador do usuário interno ou do cliente
 * @param tipo      USUARIO (equipe) ou CLIENTE
 * @param papel     perfil (ATENDENTE, COZINHA, GERENTE, ADMIN_MATRIZ) ou CLIENTE
 * @param unidadeId unidade do usuário interno; nulo para matriz e clientes
 */
public record UsuarioLogado(Integer id, String tipo, String papel, Integer unidadeId) {

    public static final String TIPO_USUARIO = "USUARIO";
    public static final String TIPO_CLIENTE = "CLIENTE";

    /** Usuário da requisição atual; vazio quando a requisição é anônima (ex.: totem). */
    public static Optional<UsuarioLogado> atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            Number uid = jwt.getClaim("uid");
            Number unidade = jwt.getClaim("unidadeId");
            List<String> papeis = jwt.getClaimAsStringList("roles");
            String papel = (papeis == null || papeis.isEmpty()) ? null : papeis.get(0);
            return Optional.of(new UsuarioLogado(uid.intValue(), jwt.getClaimAsString("tipo"), papel,
                    unidade == null ? null : unidade.intValue()));
        }
        return Optional.empty();
    }

    public boolean isCliente() {
        return TIPO_CLIENTE.equals(tipo);
    }

    public boolean isUsuarioInterno() {
        return TIPO_USUARIO.equals(tipo);
    }

    public boolean isMatriz() {
        return "ADMIN_MATRIZ".equals(papel);
    }
}

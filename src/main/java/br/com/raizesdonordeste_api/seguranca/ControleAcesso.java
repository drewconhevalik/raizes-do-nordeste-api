package br.com.raizesdonordeste_api.seguranca;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;

/**
 * Regras de escopo por unidade (RN15): usuários de unidade operam apenas dados da
 * própria unidade; a matriz acessa todas.
 */
public final class ControleAcesso {

    private ControleAcesso() {
    }

    public static UsuarioLogado exigirAutenticado() {
        return UsuarioLogado.atual()
                .orElseThrow(() -> new AccessDeniedException("Operação exige autenticação."));
    }

    public static UsuarioLogado exigirUsuarioInterno() {
        UsuarioLogado usuario = exigirAutenticado();
        if (!usuario.isUsuarioInterno()) {
            throw new AccessDeniedException("Operação restrita à equipe da rede.");
        }
        return usuario;
    }

    /** Garante que o usuário interno pertence à unidade informada (ou é da matriz). */
    public static UsuarioLogado verificarUnidade(Integer unidadeId) {
        UsuarioLogado usuario = exigirUsuarioInterno();
        if (!usuario.isMatriz() && !Objects.equals(usuario.unidadeId(), unidadeId)) {
            throw new AccessDeniedException("Usuário sem acesso à unidade " + unidadeId + ".");
        }
        return usuario;
    }

    public static boolean temPapel(UsuarioLogado usuario, String... papeis) {
        for (String papel : papeis) {
            if (papel.equals(usuario.papel())) {
                return true;
            }
        }
        return false;
    }
}

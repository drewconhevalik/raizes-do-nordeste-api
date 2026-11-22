package br.com.raizesdonordeste_api.auditoria;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.seguranca.UsuarioLogado;
import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.usuario.Usuario;
import br.com.raizesdonordeste_api.usuario.UsuarioRepository;

/**
 * Registra operações sensíveis (RF20). Roda dentro da mesma transação da operação
 * auditada: se a operação falhar, o registro também não é gravado, e vice-versa.
 * Não há método de alteração nem de exclusão (RN16).
 */
@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(RegistroAuditoriaRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(AcaoAuditoria acao, Unidade unidade, String entidade, Integer entidadeId,
            String valorAnterior, String valorNovo, String motivo) {
        Usuario autor = UsuarioLogado.atual()
                .filter(UsuarioLogado::isUsuarioInterno)
                .map(u -> usuarioRepository.getReferenceById(u.id()))
                .orElse(null);
        repository.save(new RegistroAuditoria(autor, unidade, acao, entidade, entidadeId, valorAnterior, valorNovo,
                motivo));
    }
}

package br.com.raizesdonordeste_api.auditoria;

import java.time.LocalDate;
import java.util.List;

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

    /** Consulta da trilha (RF21), do mais recente para o mais antigo; padrão: últimos 30 dias. */
    @Transactional(readOnly = true)
    public List<RegistroAuditoriaResponse> consultar(LocalDate inicio, LocalDate fim, AcaoAuditoria acao,
            Integer unidadeId) {
        LocalDate f = fim != null ? fim : LocalDate.now();
        LocalDate i = inicio != null ? inicio : f.minusDays(30);
        return repository.findTop500ByDataHoraBetweenOrderByDataHoraDesc(i.atStartOfDay(), f.plusDays(1).atStartOfDay())
                .stream()
                .filter(r -> acao == null || r.getAcao() == acao)
                .filter(r -> unidadeId == null || (r.getUnidade() != null && unidadeId.equals(r.getUnidade().getId())))
                .map(RegistroAuditoriaResponse::de)
                .toList();
    }
}

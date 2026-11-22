package br.com.raizesdonordeste_api.estoque;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.AuditoriaService;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import br.com.raizesdonordeste_api.seguranca.UsuarioLogado;
import br.com.raizesdonordeste_api.usuario.Usuario;
import br.com.raizesdonordeste_api.usuario.UsuarioRepository;

/** Controle de estoque local (RF07). Toda alteração gera movimentação; ajustes manuais são auditados. */
@Service
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    public EstoqueService(EstoqueRepository estoqueRepository, MovimentacaoEstoqueRepository movimentacaoRepository,
            UsuarioRepository usuarioRepository, AuditoriaService auditoria) {
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> listar(Integer unidadeId) {
        ControleAcesso.verificarUnidade(unidadeId);
        return estoqueRepository.listarPorUnidade(unidadeId).stream().map(EstoqueResponse::de).toList();
    }

    @Transactional
    public EstoqueResponse registrarEntrada(Integer unidadeId, Integer produtoId, EntradaEstoqueRequest req) {
        UsuarioLogado usuario = ControleAcesso.verificarUnidade(unidadeId);
        Estoque estoque = buscar(unidadeId, produtoId);
        estoque.repor(req.quantidade());
        movimentacaoRepository.save(new MovimentacaoEstoque(estoque, referencia(usuario), TipoMovimentacao.ENTRADA,
                req.quantidade(), req.motivo()));
        return EstoqueResponse.de(estoque);
    }

    @Transactional
    public EstoqueResponse ajustar(Integer unidadeId, Integer produtoId, AjusteEstoqueRequest req) {
        UsuarioLogado usuario = ControleAcesso.verificarUnidade(unidadeId);
        Estoque estoque = buscar(unidadeId, produtoId);
        int anterior = estoque.getQuantidade();
        estoque.ajustarPara(req.novaQuantidade());
        movimentacaoRepository.save(new MovimentacaoEstoque(estoque, referencia(usuario), TipoMovimentacao.AJUSTE,
                req.novaQuantidade() - anterior, req.motivo()));
        auditoria.registrar(AcaoAuditoria.AJUSTE_ESTOQUE, estoque.getUnidade(), "Estoque", estoque.getId(),
                String.valueOf(anterior), String.valueOf(req.novaQuantidade()), req.motivo());
        return EstoqueResponse.de(estoque);
    }

    private Estoque buscar(Integer unidadeId, Integer produtoId) {
        return estoqueRepository.buscar(unidadeId, produtoId).orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto " + produtoId + " não possui estoque na unidade " + unidadeId + "."));
    }

    private Usuario referencia(UsuarioLogado usuario) {
        return usuarioRepository.getReferenceById(usuario.id());
    }
}

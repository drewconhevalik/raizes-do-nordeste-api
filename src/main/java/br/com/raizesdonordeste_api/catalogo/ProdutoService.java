package br.com.raizesdonordeste_api.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.AuditoriaService;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;

/** Catálogo global da franquia (RF04), mantido pela matriz. */
@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final AuditoriaService auditoria;

    public ProdutoService(ProdutoRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return repository.findAllByOrderByNome().stream().map(ProdutoResponse::de).toList();
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest req) {
        Produto produto = new Produto(req.nome(), req.descricao(), req.categoria(), req.precoBase(),
                req.exigeCozinhaCompleta());
        if (req.ativo() != null) {
            produto.setAtivo(req.ativo());
        }
        return ProdutoResponse.de(repository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Integer id, ProdutoRequest req) {
        Produto produto = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado."));
        if (produto.getPrecoBase().compareTo(req.precoBase()) != 0) {
            auditoria.registrar(AcaoAuditoria.ALTERACAO_PRECO, null, "Produto", id,
                    produto.getPrecoBase().toPlainString(), req.precoBase().toPlainString(), "Alteração do preço base");
        }
        produto.setNome(req.nome());
        produto.setDescricao(req.descricao());
        produto.setCategoria(req.categoria());
        produto.setPrecoBase(req.precoBase());
        produto.setExigeCozinhaCompleta(req.exigeCozinhaCompleta());
        if (req.ativo() != null) {
            produto.setAtivo(req.ativo());
        }
        return ProdutoResponse.de(produto);
    }
}

package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.AuditoriaService;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;
import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import br.com.raizesdonordeste_api.estoque.Estoque;
import br.com.raizesdonordeste_api.estoque.EstoqueRepository;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.unidade.UnidadeService;

/** Cardápio por unidade (RF05, RF06). */
@Service
public class CardapioService {

    private final ItemCardapioRepository cardapioRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final UnidadeService unidadeService;
    private final AuditoriaService auditoria;

    public CardapioService(ItemCardapioRepository cardapioRepository, ProdutoRepository produtoRepository,
            EstoqueRepository estoqueRepository, UnidadeService unidadeService, AuditoriaService auditoria) {
        this.cardapioRepository = cardapioRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.unidadeService = unidadeService;
        this.auditoria = auditoria;
    }

    /**
     * RN01 (sem a verificação de estoque): produto ativo no catálogo, disponibilizado pela
     * unidade, dentro da vigência e compatível com o formato da loja.
     */
    public static boolean estaDisponivel(ItemCardapio item, LocalDate data) {
        Produto produto = item.getProduto();
        boolean cozinhaOk = !produto.isExigeCozinhaCompleta() || item.getUnidade().possuiCozinhaCompleta();
        return item.isDisponivel() && produto.isAtivo() && item.estaVigente(data) && cozinhaOk;
    }

    /** Cardápio público: apenas itens que atendem à RN01, inclusive com estoque disponível. */
    @Transactional(readOnly = true)
    public List<CardapioItemResponse> cardapioDisponivel(Integer unidadeId) {
        Unidade unidade = unidadeService.buscar(unidadeId);
        if (!unidade.isAtiva()) {
            throw new RecursoNaoEncontradoException("Unidade " + unidadeId + " não encontrada.");
        }
        LocalDate hoje = LocalDate.now();
        Map<Integer, Integer> saldos = estoqueRepository.listarPorUnidade(unidadeId).stream()
                .collect(Collectors.toMap(e -> e.getProduto().getId(), Estoque::getQuantidade));
        return cardapioRepository.listarPorUnidade(unidadeId).stream()
                .filter(item -> estaDisponivel(item, hoje))
                .filter(item -> saldos.getOrDefault(item.getProduto().getId(), 0) > 0)
                .map(CardapioItemResponse::de)
                .toList();
    }

    /** Visão de gestão: todos os itens configurados, inclusive indisponíveis. */
    @Transactional(readOnly = true)
    public List<CardapioItemResponse> cardapioCompleto(Integer unidadeId) {
        ControleAcesso.verificarUnidade(unidadeId);
        return cardapioRepository.listarPorUnidade(unidadeId).stream().map(CardapioItemResponse::de).toList();
    }

    /** Inclui ou altera um produto no cardápio da unidade; alteração de preço local é auditada. */
    @Transactional
    public CardapioItemResponse configurar(Integer unidadeId, Integer produtoId, CardapioItemRequest req) {
        ControleAcesso.verificarUnidade(unidadeId);
        if (req.vigenciaInicio() != null && req.vigenciaFim() != null
                && req.vigenciaFim().isBefore(req.vigenciaInicio())) {
            throw new RegraNegocioException("O fim da vigência não pode ser anterior ao início.");
        }
        Unidade unidade = unidadeService.buscar(unidadeId);
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + produtoId + " não encontrado."));

        ItemCardapio item = cardapioRepository.buscar(unidadeId, produtoId)
                .orElseGet(() -> cardapioRepository.save(new ItemCardapio(unidade, produto)));
        if (estoqueRepository.buscar(unidadeId, produtoId).isEmpty()) {
            estoqueRepository.save(new Estoque(unidade, produto, 0));
        }

        if (!Objects.equals(valor(item.getPrecoLocal()), valor(req.precoLocal()))) {
            auditoria.registrar(AcaoAuditoria.ALTERACAO_PRECO, unidade, "ItemCardapio", item.getId(),
                    texto(item.getPrecoLocal()), texto(req.precoLocal()), "Alteração do preço local");
        }
        item.setPrecoLocal(req.precoLocal());
        item.setDisponivel(req.disponivel());
        item.setVigenciaInicio(req.vigenciaInicio());
        item.setVigenciaFim(req.vigenciaFim());
        item.setObservacaoRegional(req.observacaoRegional());
        return CardapioItemResponse.de(item);
    }

    private static String valor(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private static String texto(BigDecimal v) {
        return v == null ? "preço base" : v.toPlainString();
    }
}

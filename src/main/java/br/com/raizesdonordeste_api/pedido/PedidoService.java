package br.com.raizesdonordeste_api.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.AuditoriaService;
import br.com.raizesdonordeste_api.catalogo.CardapioService;
import br.com.raizesdonordeste_api.catalogo.ItemCardapio;
import br.com.raizesdonordeste_api.catalogo.ItemCardapioRepository;
import br.com.raizesdonordeste_api.cliente.Cliente;
import br.com.raizesdonordeste_api.cliente.ClienteRepository;
import br.com.raizesdonordeste_api.cliente.Finalidade;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;
import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import br.com.raizesdonordeste_api.estoque.Estoque;
import br.com.raizesdonordeste_api.estoque.EstoqueRepository;
import br.com.raizesdonordeste_api.estoque.MovimentacaoEstoque;
import br.com.raizesdonordeste_api.estoque.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste_api.estoque.TipoMovimentacao;
import br.com.raizesdonordeste_api.fidelidade.MovimentoPontos;
import br.com.raizesdonordeste_api.fidelidade.MovimentoPontosRepository;
import br.com.raizesdonordeste_api.fidelidade.RegrasFidelidade;
import br.com.raizesdonordeste_api.fidelidade.TipoMovimentoPontos;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import br.com.raizesdonordeste_api.seguranca.UsuarioLogado;
import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.unidade.UnidadeRepository;
import br.com.raizesdonordeste_api.usuario.Usuario;
import br.com.raizesdonordeste_api.usuario.UsuarioRepository;

/** Regras de negócio do pedido (RF08–RF10, RF13, RF14, RF16). */
@Service
public class PedidoService {

    /** RN07: limite do desconto manual sobre o valor dos itens. */
    static final BigDecimal LIMITE_DESCONTO_MANUAL = new BigDecimal("0.20");

    private static final EnumSet<StatusPedido> FILA_ATIVA =
            EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO);

    private final PedidoRepository pedidoRepository;
    private final UnidadeRepository unidadeRepository;
    private final ItemCardapioRepository cardapioRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentoPontosRepository movimentoPontosRepository;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public PedidoService(PedidoRepository pedidoRepository, UnidadeRepository unidadeRepository,
            ItemCardapioRepository cardapioRepository, EstoqueRepository estoqueRepository,
            MovimentacaoEstoqueRepository movimentacaoRepository, ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository, MovimentoPontosRepository movimentoPontosRepository,
            AuditoriaService auditoria, ApplicationEventPublisher eventos) {
        this.pedidoRepository = pedidoRepository;
        this.unidadeRepository = unidadeRepository;
        this.cardapioRepository = cardapioRepository;
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentoPontosRepository = movimentoPontosRepository;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    /**
     * Cria o pedido validando o cardápio (RN01) e reservando o estoque (RN03) na mesma
     * transação: ou tudo é gravado, ou nada é.
     */
    @Transactional
    public PedidoResponse criar(CriarPedidoRequest req) {
        Unidade unidade = unidadeRepository.findById(req.unidadeId())
                .filter(Unidade::isAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade " + req.unidadeId() + " não encontrada."));

        Optional<UsuarioLogado> logado = UsuarioLogado.atual();
        Cliente cliente = null;
        Usuario atendente = null;
        if (logado.isPresent() && logado.get().isCliente()) {
            cliente = clienteRepository.findById(logado.get().id())
                    .filter(c -> !c.isAnonimizado())
                    .orElseThrow(() -> new AccessDeniedException("Cliente inválido."));
        } else if (logado.isPresent() && logado.get().isUsuarioInterno()) {
            ControleAcesso.verificarUnidade(unidade.getId());
            atendente = usuarioRepository.getReferenceById(logado.get().id());
        }
        validarCanal(req.canal(), cliente, atendente);

        Pedido pedido = new Pedido(unidade, cliente, atendente, req.canal(), req.tipoAtendimento());
        LocalDate hoje = LocalDate.now();

        // Agrupa itens repetidos e ordena por produto (ordem fixa de bloqueio, evita impasses)
        Map<Integer, Integer> quantidades = new TreeMap<>();
        req.itens().forEach(i -> quantidades.merge(i.produtoId(), i.quantidade(), Integer::sum));

        Map<Estoque, Integer> baixas = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entrada : quantidades.entrySet()) {
            Integer produtoId = entrada.getKey();
            int quantidade = entrada.getValue();
            ItemCardapio item = cardapioRepository.buscar(unidade.getId(), produtoId)
                    .orElseThrow(() -> new RegraNegocioException(
                            "O produto " + produtoId + " não faz parte do cardápio desta unidade."));
            if (!CardapioService.estaDisponivel(item, hoje)) {
                throw new RegraNegocioException(
                        "O produto '" + item.getProduto().getNome() + "' está indisponível nesta unidade.");
            }
            Estoque estoque = estoqueRepository.buscar(unidade.getId(), produtoId)
                    .orElseThrow(() -> new RegraNegocioException(
                            "O produto '" + item.getProduto().getNome() + "' está sem estoque."));
            estoque.baixar(quantidade); // RN03: lança 422 se insuficiente
            baixas.put(estoque, quantidade);
            pedido.adicionarItem(item.getProduto(), quantidade, item.precoVigente());
        }

        int pontos = req.pontosResgatar() == null ? 0 : req.pontosResgatar();
        if (pontos > 0) {
            resgatarPontos(pedido, cliente, pontos);
        }

        pedidoRepository.save(pedido);
        for (Map.Entry<Estoque, Integer> baixa : baixas.entrySet()) {
            movimentacaoRepository.save(new MovimentacaoEstoque(baixa.getKey(), atendente,
                    TipoMovimentacao.SAIDA_PEDIDO, -baixa.getValue(), "Pedido #" + pedido.getId()));
        }
        if (pontos > 0) {
            movimentoPontosRepository.save(new MovimentoPontos(cliente, pedido, TipoMovimentoPontos.RESGATE, -pontos));
        }
        return PedidoResponse.de(pedido);
    }

    /** APP exige cliente identificado; BALCAO exige atendente da unidade; TOTEM aceita anônimo. */
    private void validarCanal(CanalPedido canal, Cliente cliente, Usuario atendente) {
        if (canal == CanalPedido.APP && cliente == null) {
            throw new RegraNegocioException("Pedidos pelo aplicativo exigem cliente autenticado.");
        }
        if (canal == CanalPedido.BALCAO && atendente == null) {
            throw new RegraNegocioException("Pedidos de balcão devem ser registrados pela equipe da unidade.");
        }
    }

    /** RF16 / RN09: resgate em blocos de 100 pontos, até 50% do valor dos itens; exige consentimento. */
    private void resgatarPontos(Pedido pedido, Cliente cliente, int pontos) {
        if (cliente == null) {
            throw new RegraNegocioException("Resgate de pontos exige cliente identificado.");
        }
        if (!cliente.possuiConsentimento(Finalidade.FIDELIDADE)) {
            throw new RegraNegocioException("O cliente não possui consentimento ativo para o programa de fidelidade.");
        }
        if (pontos % RegrasFidelidade.PONTOS_POR_BLOCO != 0) {
            throw new RegraNegocioException("O resgate deve ser feito em blocos de "
                    + RegrasFidelidade.PONTOS_POR_BLOCO + " pontos.");
        }
        BigDecimal desconto = RegrasFidelidade.valorDoResgate(pontos);
        BigDecimal limite = pedido.getValorItens().multiply(RegrasFidelidade.LIMITE_RESGATE)
                .setScale(2, RoundingMode.HALF_UP);
        if (desconto.compareTo(limite) > 0) {
            throw new RegraNegocioException("O resgate (R$ " + desconto + ") excede 50% do valor dos itens (limite R$ "
                    + limite + ").");
        }
        cliente.debitarPontos(pontos);
        pedido.registrarResgate(pontos, desconto);
    }

    @Transactional(readOnly = true)
    public PedidoResponse consultar(Integer id) {
        Pedido pedido = buscar(id);
        UsuarioLogado usuario = ControleAcesso.exigirAutenticado();
        if (usuario.isCliente()) {
            if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(usuario.id())) {
                throw new AccessDeniedException("O pedido não pertence a este cliente.");
            }
        } else {
            ControleAcesso.verificarUnidade(pedido.getUnidade().getId());
        }
        return PedidoResponse.de(pedido);
    }

    /** Acompanhamento público por código (pedidos anônimos do totem). */
    @Transactional(readOnly = true)
    public PedidoResponse acompanhar(String codigo) {
        return pedidoRepository.findByCodigoAcompanhamento(codigo).map(PedidoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    /** Fila da unidade; sem filtro, devolve os pedidos em andamento (pagos, em preparo, prontos). */
    @Transactional(readOnly = true)
    public List<PedidoResponse> fila(Integer unidadeId, StatusPedido status) {
        ControleAcesso.verificarUnidade(unidadeId);
        EnumSet<StatusPedido> filtro = status == null ? FILA_ATIVA : EnumSet.of(status);
        return pedidoRepository.fila(unidadeId, filtro).stream().map(PedidoResponse::de).toList();
    }

    /**
     * Avança o status operacional (RF10). PAGO só é definido pelo pagamento e CANCELADO
     * pelo endpoint de cancelamento. Cozinha: EM_PREPARO e PRONTO; atendente: ENTREGUE.
     */
    @Transactional
    public PedidoResponse alterarStatus(Integer id, StatusPedido novo) {
        if (novo == StatusPedido.PAGO || novo == StatusPedido.CANCELADO || novo == StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("O status " + novo + " não pode ser definido manualmente.");
        }
        Pedido pedido = buscar(id);
        UsuarioLogado usuario = ControleAcesso.verificarUnidade(pedido.getUnidade().getId());
        boolean permitido = switch (novo) {
            case EM_PREPARO, PRONTO -> ControleAcesso.temPapel(usuario, "COZINHA", "GERENTE", "ADMIN_MATRIZ");
            case ENTREGUE -> ControleAcesso.temPapel(usuario, "ATENDENTE", "GERENTE", "ADMIN_MATRIZ");
            default -> false;
        };
        if (!permitido) {
            throw new AccessDeniedException("Perfil " + usuario.papel() + " não pode definir o status " + novo + ".");
        }
        pedido.alterarStatus(novo, usuarioRepository.getReferenceById(usuario.id()));
        return PedidoResponse.de(pedido);
    }

    /**
     * Cancelamento (RF13, RN06): devolve o estoque, registra auditoria e publica evento para
     * estorno do pagamento e dos pontos. Pedido pago ou em preparo só o gerente cancela.
     */
    @Transactional
    public PedidoResponse cancelar(Integer id, String motivo) {
        Pedido pedido = buscar(id);
        UsuarioLogado usuario = ControleAcesso.verificarUnidade(pedido.getUnidade().getId());
        StatusPedido anterior = pedido.getStatus();
        if (anterior != StatusPedido.AGUARDANDO_PAGAMENTO
                && !ControleAcesso.temPapel(usuario, "GERENTE", "ADMIN_MATRIZ")) {
            throw new AccessDeniedException("Pedidos pagos ou em preparo só podem ser cancelados pelo gerente.");
        }
        Usuario responsavel = usuarioRepository.getReferenceById(usuario.id());
        pedido.alterarStatus(StatusPedido.CANCELADO, responsavel); // 409 se PRONTO ou ENTREGUE
        devolverEstoque(pedido, responsavel);
        auditoria.registrar(AcaoAuditoria.CANCELAMENTO_PEDIDO, pedido.getUnidade(), "Pedido", pedido.getId(),
                anterior.name(), StatusPedido.CANCELADO.name(), motivo);
        eventos.publishEvent(new PedidoCanceladoEvent(pedido.getId(), anterior));
        return PedidoResponse.de(pedido);
    }

    /** Desconto manual do gerente (RF14, RN07): motivo obrigatório, até 20% dos itens, auditado. */
    @Transactional
    public PedidoResponse aplicarDesconto(Integer id, DescontoRequest req) {
        Pedido pedido = buscar(id);
        ControleAcesso.verificarUnidade(pedido.getUnidade().getId());
        BigDecimal limite = pedido.getValorItens().multiply(LIMITE_DESCONTO_MANUAL).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descontoManualAtual = pedido.getValorDesconto()
                .subtract(RegrasFidelidade.valorDoResgate(pedido.getPontosResgatados()));
        if (descontoManualAtual.add(req.valor()).compareTo(limite) > 0) {
            throw new RegraNegocioException("O desconto manual não pode passar de 20% do valor dos itens (limite R$ "
                    + limite + ").");
        }
        String totalAnterior = pedido.getValorTotal().toPlainString();
        pedido.aplicarDesconto(req.valor());
        auditoria.registrar(AcaoAuditoria.DESCONTO_PEDIDO, pedido.getUnidade(), "Pedido", pedido.getId(),
                totalAnterior, pedido.getValorTotal().toPlainString(), req.motivo());
        return PedidoResponse.de(pedido);
    }

    /** Devolve ao estoque os itens de um pedido cancelado (também usado na expiração, RN04). */
    void devolverEstoque(Pedido pedido, Usuario responsavel) {
        for (ItemPedido item : pedido.getItens()) {
            estoqueRepository.buscar(pedido.getUnidade().getId(), item.getProduto().getId()).ifPresent(estoque -> {
                estoque.repor(item.getQuantidade());
                movimentacaoRepository.save(new MovimentacaoEstoque(estoque, responsavel,
                        TipoMovimentacao.DEVOLUCAO_PEDIDO, item.getQuantidade(), "Cancelamento do pedido #" + pedido.getId()));
            });
        }
    }

    Pedido buscar(Integer id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + id + " não encontrado."));
    }
}

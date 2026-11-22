package br.com.raizesdonordeste_api.pagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.raizesdonordeste_api.comum.ConflitoException;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;
import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import br.com.raizesdonordeste_api.comum.ServicoIndisponivelException;
import br.com.raizesdonordeste_api.fidelidade.FidelidadeService;
import br.com.raizesdonordeste_api.pedido.Pedido;
import br.com.raizesdonordeste_api.pedido.PedidoCanceladoEvent;
import br.com.raizesdonordeste_api.pedido.PedidoRepository;
import br.com.raizesdonordeste_api.pedido.PedidoService;
import br.com.raizesdonordeste_api.pedido.StatusPedido;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import br.com.raizesdonordeste_api.seguranca.UsuarioLogado;

/**
 * Integração com o serviço de pagamento (RF11, RF12). A chamada externa acontece FORA de
 * transação de banco: (1) registra a tentativa como PENDENTE; (2) chama o serviço com tempo
 * limite; (3) grava o resultado. Assim uma demora do serviço externo não prende conexões do
 * banco, e uma falha não desfaz o pedido (RNF05).
 */
@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final FidelidadeService fidelidadeService;
    private final GatewayPagamento gateway;
    private final TransactionTemplate transacao;
    private final long tempoLimiteMs;

    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoRepository pedidoRepository,
            PedidoService pedidoService, FidelidadeService fidelidadeService, GatewayPagamento gateway,
            PlatformTransactionManager transactionManager, @Value("${app.gateway.timeout-ms}") long tempoLimiteMs) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
        this.fidelidadeService = fidelidadeService;
        this.gateway = gateway;
        this.transacao = new TransactionTemplate(transactionManager);
        this.tempoLimiteMs = tempoLimiteMs;
    }

    /** Resultado da solicitação: {@code criado = false} quando a chave de idempotência já existia. */
    public record Resultado(PagamentoResponse pagamento, boolean criado) {
    }

    private record Preparacao(PagamentoResponse existente, Integer pagamentoId, BigDecimal valor) {
    }

    public Resultado iniciar(Integer pedidoId, FormaPagamento forma, String chaveIdempotencia) {
        Preparacao preparacao = transacao.execute(status -> preparar(pedidoId, forma, chaveIdempotencia));
        if (preparacao.existente() != null) {
            return new Resultado(preparacao.existente(), false); // RN14: repetição não gera nova cobrança
        }
        RespostaGateway resposta = chamarServicoExterno(
                new SolicitacaoPagamento(chaveIdempotencia, preparacao.valor(), forma));
        PagamentoResponse pagamento = transacao.execute(status -> aplicarResultado(preparacao.pagamentoId(), resposta));
        if (pagamento.status() == StatusPagamento.ERRO) {
            throw new ServicoIndisponivelException("Serviço de pagamento indisponível: " + resposta.mensagem()
                    + " O pedido foi preservado; tente novamente com uma nova Idempotency-Key.");
        }
        return new Resultado(pagamento, true);
    }

    private Preparacao preparar(Integer pedidoId, FormaPagamento forma, String chave) {
        var existente = pagamentoRepository.findByChaveIdempotencia(chave);
        if (existente.isPresent()) {
            if (!existente.get().getPedido().getId().equals(pedidoId)) {
                throw new ConflitoException("A Idempotency-Key informada já foi usada em outro pedido.");
            }
            return new Preparacao(PagamentoResponse.de(existente.get()), null, null);
        }
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + pedidoId + " não encontrado."));
        verificarQuemPaga(pedido);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("O pedido está " + pedido.getStatus() + " e não aceita pagamento.");
        }
        boolean temPendente = pagamentoRepository.findByPedidoIdOrderByIdAsc(pedidoId).stream()
                .anyMatch(p -> p.getStatus() == StatusPagamento.PENDENTE);
        if (temPendente) {
            throw new ConflitoException("Já existe um pagamento pendente de confirmação para este pedido.");
        }
        if (pedido.getValorTotal().signum() <= 0) {
            throw new RegraNegocioException("O pedido não possui valor a pagar.");
        }
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, chave, forma, pedido.getValorTotal()));
        return new Preparacao(null, pagamento.getId(), pagamento.getValor());
    }

    /** Pedido de cliente identificado só pode ser pago pelo próprio cliente ou pela equipe da unidade. */
    private void verificarQuemPaga(Pedido pedido) {
        if (pedido.getCliente() == null) {
            return; // pedido anônimo do totem
        }
        UsuarioLogado usuario = ControleAcesso.exigirAutenticado();
        if (usuario.isCliente()) {
            if (!usuario.id().equals(pedido.getCliente().getId())) {
                throw new AccessDeniedException("O pedido não pertence a este cliente.");
            }
        } else {
            ControleAcesso.verificarUnidade(pedido.getUnidade().getId());
        }
    }

    /** Chamada ao serviço externo com tempo limite; falha técnica vira resultado ERRO. */
    private RespostaGateway chamarServicoExterno(SolicitacaoPagamento solicitacao) {
        try {
            return CompletableFuture.supplyAsync(() -> gateway.solicitarPagamento(solicitacao))
                    .get(tempoLimiteMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return RespostaGateway.erro("tempo limite de " + tempoLimiteMs + " ms excedido.");
        } catch (ExecutionException e) {
            return RespostaGateway.erro(e.getCause() != null ? e.getCause().getMessage() : "falha na comunicação.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RespostaGateway.erro("operação interrompida.");
        }
    }

    private PagamentoResponse aplicarResultado(Integer pagamentoId, RespostaGateway resposta) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId).orElseThrow();
        switch (resposta.resultado()) {
            case APROVADO -> confirmar(pagamento, resposta.idTransacao());
            case RECUSADO -> pagamento.atualizar(StatusPagamento.RECUSADO, resposta.idTransacao());
            case PENDENTE -> pagamento.atualizar(StatusPagamento.PENDENTE, resposta.idTransacao());
            case ERRO -> pagamento.atualizar(StatusPagamento.ERRO, null);
        }
        return PagamentoResponse.de(pagamento);
    }

    /**
     * Aprovação: o pedido passa a PAGO e os pontos são creditados. Se o pedido já foi cancelado
     * ou expirou (aprovação tardia), a aprovação é registrada e o estorno é solicitado (RN14).
     */
    private void confirmar(Pagamento pagamento, String idTransacao) {
        pagamento.atualizar(StatusPagamento.APROVADO, idTransacao);
        Pedido pedido = pagamento.getPedido();
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
            pedido.alterarStatus(StatusPedido.PAGO, null);
            fidelidadeService.creditarPorPagamento(pedido);
        } else {
            gateway.solicitarEstorno(pagamento.getIdTransacaoExterna());
            pagamento.atualizar(StatusPagamento.ESTORNO_SOLICITADO, null);
        }
    }

    /** Webhook (RF12): idempotente — notificação repetida de um pagamento já finalizado não muda nada. */
    @Transactional
    public PagamentoResponse processarWebhook(WebhookRequest notificacao) {
        if (notificacao.status() != ResultadoGateway.APROVADO && notificacao.status() != ResultadoGateway.RECUSADO) {
            throw new RegraNegocioException("O webhook aceita apenas os resultados APROVADO ou RECUSADO.");
        }
        Pagamento pagamento = pagamentoRepository.findByIdTransacaoExterna(notificacao.idTransacao())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada."));
        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            return PagamentoResponse.de(pagamento);
        }
        return aplicarResultado(pagamento.getId(),
                new RespostaGateway(notificacao.status(), notificacao.idTransacao(), "webhook"));
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(Integer pedidoId) {
        pedidoService.consultar(pedidoId); // aplica as mesmas regras de acesso da consulta do pedido
        return pagamentoRepository.findByPedidoIdOrderByIdAsc(pedidoId).stream().map(PagamentoResponse::de).toList();
    }

    /** Cancelamento de pedido pago ou em preparo: solicita o estorno das transações aprovadas (RN06). */
    @EventListener
    public void aoCancelarPedido(PedidoCanceladoEvent evento) {
        if (evento.statusAnterior() != StatusPedido.PAGO && evento.statusAnterior() != StatusPedido.EM_PREPARO) {
            return;
        }
        for (Pagamento pagamento : pagamentoRepository.findByPedidoIdOrderByIdAsc(evento.pedidoId())) {
            if (pagamento.getStatus() == StatusPagamento.APROVADO) {
                gateway.solicitarEstorno(pagamento.getIdTransacaoExterna());
                pagamento.atualizar(StatusPagamento.ESTORNO_SOLICITADO, null);
            }
        }
    }
}

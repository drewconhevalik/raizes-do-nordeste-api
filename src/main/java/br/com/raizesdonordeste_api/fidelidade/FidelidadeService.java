package br.com.raizesdonordeste_api.fidelidade;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.cliente.Cliente;
import br.com.raizesdonordeste_api.cliente.ClienteRepository;
import br.com.raizesdonordeste_api.cliente.Finalidade;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;
import br.com.raizesdonordeste_api.pedido.Pedido;
import br.com.raizesdonordeste_api.pedido.PedidoCanceladoEvent;
import br.com.raizesdonordeste_api.pedido.PedidoRepository;

/** Programa de fidelidade: crédito de pontos no pagamento (RF15, RN08) e estornos no cancelamento. */
@Service
public class FidelidadeService {

    private final MovimentoPontosRepository movimentoRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

    public FidelidadeService(MovimentoPontosRepository movimentoRepository, PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository) {
        this.movimentoRepository = movimentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
    }

    /** RN08: só cliente identificado e com consentimento de fidelidade acumula pontos. */
    @Transactional
    public void creditarPorPagamento(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        if (cliente == null || !cliente.possuiConsentimento(Finalidade.FIDELIDADE)) {
            return;
        }
        int pontos = RegrasFidelidade.pontosGanhos(pedido.getValorTotal());
        if (pontos > 0) {
            cliente.creditarPontos(pontos);
            movimentoRepository.save(new MovimentoPontos(cliente, pedido, TipoMovimentoPontos.CREDITO, pontos));
        }
    }

    /**
     * Ao cancelar um pedido: devolve os pontos resgatados nele e retira os pontos ganhos com ele.
     * Se o cliente já usou os pontos ganhos, retira-se apenas o saldo disponível (o saldo nunca fica negativo).
     */
    @EventListener
    public void aoCancelarPedido(PedidoCanceladoEvent evento) {
        Pedido pedido = pedidoRepository.findById(evento.pedidoId()).orElse(null);
        if (pedido == null || pedido.getCliente() == null) {
            return;
        }
        Cliente cliente = pedido.getCliente();
        if (pedido.getPontosResgatados() > 0) {
            cliente.creditarPontos(pedido.getPontosResgatados());
            movimentoRepository.save(new MovimentoPontos(cliente, pedido, TipoMovimentoPontos.ESTORNO,
                    pedido.getPontosResgatados()));
        }
        int ganhos = movimentoRepository.findByPedidoIdAndTipo(pedido.getId(), TipoMovimentoPontos.CREDITO).stream()
                .mapToInt(MovimentoPontos::getPontos).sum();
        int estorno = Math.min(ganhos, cliente.getSaldoPontos());
        if (estorno > 0) {
            cliente.debitarPontos(estorno);
            movimentoRepository.save(new MovimentoPontos(cliente, pedido, TipoMovimentoPontos.ESTORNO, -estorno));
        }
    }

    @Transactional(readOnly = true)
    public PontosResponse extrato(Integer clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        var movimentos = movimentoRepository.findByClienteIdOrderByDataHoraDesc(clienteId).stream()
                .map(m -> new PontosResponse.Movimento(m.getTipo(), m.getPontos(),
                        m.getPedido() != null ? m.getPedido().getId() : null, m.getDataHora()))
                .toList();
        return new PontosResponse(cliente.getSaldoPontos(), movimentos);
    }
}

package br.com.raizesdonordeste_api.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import br.com.raizesdonordeste_api.pedido.ItemPedidoRepository;
import br.com.raizesdonordeste_api.pedido.PedidoRepository;
import br.com.raizesdonordeste_api.pedido.StatusPedido;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import br.com.raizesdonordeste_api.seguranca.UsuarioLogado;

/**
 * Relatórios gerenciais (RF22, RF23). Consideram apenas pedidos pagos (pagos, em preparo,
 * prontos ou entregues). O gerente vê somente a própria unidade; a matriz vê todas.
 * Em produção, estas consultas leriam de uma réplica do banco (docs/03-arquitetura.md).
 */
@Service
public class RelatorioService {

    private static final EnumSet<StatusPedido> VENDIDOS =
            EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO, StatusPedido.ENTREGUE);

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemRepository;

    public RelatorioService(PedidoRepository pedidoRepository, ItemPedidoRepository itemRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioResponse vendas(LocalDate inicio, LocalDate fim, String agrupamento, Integer unidadeId) {
        Periodo periodo = periodo(inicio, fim);
        Integer escopo = escopoDaUnidade(unidadeId);
        String agrupar = agrupamento == null ? "UNIDADE" : agrupamento.toUpperCase();
        List<Object[]> linhas;
        if (escopo != null) {
            agrupar = "UNIDADE";
            linhas = pedidoRepository.vendasDaUnidade(VENDIDOS, periodo.inicioDia(), periodo.fimExclusivo(), escopo);
        } else if (agrupar.equals("REGIAO")) {
            linhas = pedidoRepository.vendasPorRegiao(VENDIDOS, periodo.inicioDia(), periodo.fimExclusivo());
        } else if (agrupar.equals("UNIDADE")) {
            linhas = pedidoRepository.vendasPorUnidade(VENDIDOS, periodo.inicioDia(), periodo.fimExclusivo());
        } else {
            throw new RegraNegocioException("Agrupamento inválido: use UNIDADE ou REGIAO.");
        }
        return montar("VENDAS", periodo, agrupar, linhas);
    }

    @Transactional(readOnly = true)
    public RelatorioResponse produtosMaisVendidos(LocalDate inicio, LocalDate fim, Integer unidadeId, int limite) {
        Periodo periodo = periodo(inicio, fim);
        Integer escopo = escopoDaUnidade(unidadeId);
        PageRequest pagina = PageRequest.of(0, Math.max(1, Math.min(limite, 50)));
        List<Object[]> linhas = escopo == null
                ? itemRepository.maisVendidos(VENDIDOS, periodo.inicioDia(), periodo.fimExclusivo(), pagina)
                : itemRepository.maisVendidosDaUnidade(VENDIDOS, periodo.inicioDia(), periodo.fimExclusivo(), escopo,
                        pagina);
        return montar("PRODUTOS_MAIS_VENDIDOS", periodo, "PRODUTO", linhas);
    }

    /** Gerente: força o filtro na própria unidade; matriz: usa o filtro informado (ou todas). */
    private Integer escopoDaUnidade(Integer unidadeId) {
        UsuarioLogado usuario = ControleAcesso.exigirUsuarioInterno();
        if (usuario.isMatriz()) {
            return unidadeId;
        }
        if (unidadeId != null && !unidadeId.equals(usuario.unidadeId())) {
            throw new AccessDeniedException("O gerente só consulta relatórios da própria unidade.");
        }
        return usuario.unidadeId();
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {
        LocalDateTime inicioDia() {
            return inicio.atStartOfDay();
        }

        LocalDateTime fimExclusivo() {
            return fim.plusDays(1).atStartOfDay();
        }
    }

    /** Padrão: últimos 30 dias. */
    private static Periodo periodo(LocalDate inicio, LocalDate fim) {
        LocalDate f = fim != null ? fim : LocalDate.now();
        LocalDate i = inicio != null ? inicio : f.minusDays(30);
        if (i.isAfter(f)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à final.");
        }
        return new Periodo(i, f);
    }

    private static RelatorioResponse montar(String nome, Periodo periodo, String agrupamento, List<Object[]> linhas) {
        List<RelatorioResponse.Linha> resultado = linhas.stream()
                .map(l -> new RelatorioResponse.Linha(String.valueOf(l[0]), ((Number) l[1]).longValue(),
                        (BigDecimal) l[2]))
                .toList();
        long quantidade = resultado.stream().mapToLong(RelatorioResponse.Linha::quantidade).sum();
        BigDecimal valor = resultado.stream().map(RelatorioResponse.Linha::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatorioResponse(nome, periodo.inicio(), periodo.fim(), agrupamento, resultado, quantidade, valor);
    }
}

package br.com.raizesdonordeste_api.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.raizesdonordeste_api.catalogo.Produto;
import br.com.raizesdonordeste_api.cliente.Cliente;
import br.com.raizesdonordeste_api.comum.ConflitoException;
import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.usuario.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "pedido", indexes = {
        @Index(name = "idx_pedido_unidade_status", columnList = "unidade_id, status"),
        @Index(name = "idx_pedido_unidade_criado", columnList = "unidade_id, criado_em")
})
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    /** Nulo para pedido anônimo (totem ou balcão sem identificação, RN13). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Usuário que registrou o pedido no balcão; nulo no autoatendimento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalPedido canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_atendimento", nullable = false, length = 20)
    private TipoAtendimento tipoAtendimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Column(name = "valor_itens", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorItens = BigDecimal.ZERO.setScale(2);

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto = BigDecimal.ZERO.setScale(2);

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "pontos_resgatados", nullable = false)
    private int pontosResgatados;

    /** Código público para acompanhar pedidos anônimos sem expor o identificador interno. */
    @Column(name = "codigo_acompanhamento", nullable = false, unique = true, length = 36)
    private String codigoAcompanhamento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Version
    private Integer version;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("dataHora ASC, id ASC")
    private List<HistoricoStatusPedido> historico = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Unidade unidade, Cliente cliente, Usuario criadoPor, CanalPedido canal,
            TipoAtendimento tipoAtendimento) {
        this.unidade = unidade;
        this.cliente = cliente;
        this.criadoPor = criadoPor;
        this.canal = canal;
        this.tipoAtendimento = tipoAtendimento;
        this.status = StatusPedido.AGUARDANDO_PAGAMENTO;
        this.codigoAcompanhamento = UUID.randomUUID().toString();
        this.criadoEm = LocalDateTime.now();
        this.historico.add(new HistoricoStatusPedido(this, null, StatusPedido.AGUARDANDO_PAGAMENTO, criadoPor));
    }

    public void adicionarItem(Produto produto, int quantidade, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade de cada item deve ser maior que zero.");
        }
        itens.add(new ItemPedido(this, produto, quantidade, precoUnitario));
        recalcular();
    }

    /** Soma um desconto ao pedido (desconto manual do gerente ou resgate de pontos). */
    public void aplicarDesconto(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("O valor do desconto deve ser maior que zero.");
        }
        if (status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("Desconto só pode ser aplicado antes do pagamento.");
        }
        valorDesconto = valorDesconto.add(valor).setScale(2, RoundingMode.HALF_UP);
        recalcular();
    }

    public void registrarResgate(int pontos, BigDecimal valorDescontoPontos) {
        this.pontosResgatados += pontos;
        aplicarDesconto(valorDescontoPontos);
    }

    /** Muda o status validando o ciclo de vida (RN05) e registrando o histórico. */
    public void alterarStatus(StatusPedido novo, Usuario responsavel) {
        if (!status.podeIrPara(novo)) {
            throw new ConflitoException("Transição de status inválida: " + status + " -> " + novo + ".");
        }
        historico.add(new HistoricoStatusPedido(this, status, novo, responsavel));
        status = novo;
    }

    private void recalcular() {
        valorItens = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = valorItens.subtract(valorDesconto);
        valorTotal = (total.signum() < 0 ? BigDecimal.ZERO : total).setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Cliente getCliente() { return cliente; }
    public Usuario getCriadoPor() { return criadoPor; }
    public CanalPedido getCanal() { return canal; }
    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public StatusPedido getStatus() { return status; }
    public BigDecimal getValorItens() { return valorItens; }
    public BigDecimal getValorDesconto() { return valorDesconto; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public int getPontosResgatados() { return pontosResgatados; }
    public String getCodigoAcompanhamento() { return codigoAcompanhamento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Integer getVersion() { return version; }
    public List<ItemPedido> getItens() { return itens; }
    public List<HistoricoStatusPedido> getHistorico() { return historico; }
}

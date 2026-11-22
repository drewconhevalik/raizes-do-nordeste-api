package br.com.raizesdonordeste_api.pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.raizesdonordeste_api.pedido.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma tentativa de pagamento de um pedido. A chave de idempotência é única: a mesma
 * solicitação enviada duas vezes devolve o registro existente, sem nova cobrança (RN14).
 */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 100)
    private String chaveIdempotencia;

    @Column(name = "id_transacao_externa", length = 100)
    private String idTransacaoExterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento forma;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Pagamento() {
    }

    public Pagamento(Pedido pedido, String chaveIdempotencia, FormaPagamento forma, BigDecimal valor) {
        this.pedido = pedido;
        this.chaveIdempotencia = chaveIdempotencia;
        this.forma = forma;
        this.valor = valor;
        this.status = StatusPagamento.PENDENTE;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void atualizar(StatusPagamento novoStatus, String idTransacaoExterna) {
        this.status = novoStatus;
        if (idTransacaoExterna != null) {
            this.idTransacaoExterna = idTransacaoExterna;
        }
        this.atualizadoEm = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public String getIdTransacaoExterna() { return idTransacaoExterna; }
    public FormaPagamento getForma() { return forma; }
    public BigDecimal getValor() { return valor; }
    public StatusPagamento getStatus() { return status; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}

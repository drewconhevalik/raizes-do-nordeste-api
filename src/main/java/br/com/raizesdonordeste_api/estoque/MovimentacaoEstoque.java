package br.com.raizesdonordeste_api.estoque;

import java.time.LocalDateTime;

import br.com.raizesdonordeste_api.usuario.Usuario;
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

/** Histórico de toda alteração de estoque (rastreabilidade). */
@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estoque_id")
    private Estoque estoque;

    /** Nulo quando a movimentação é feita pelo sistema (ex.: pedido do totem). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(length = 255)
    private String motivo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(Estoque estoque, Usuario usuario, TipoMovimentacao tipo, int quantidade,
            String motivo) {
        this.estoque = estoque;
        this.usuario = usuario;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.dataHora = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Estoque getEstoque() { return estoque; }
    public Usuario getUsuario() { return usuario; }
    public TipoMovimentacao getTipo() { return tipo; }
    public int getQuantidade() { return quantidade; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getDataHora() { return dataHora; }
}

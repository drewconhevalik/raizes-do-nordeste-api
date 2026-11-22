package br.com.raizesdonordeste_api.pedido;

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

/** Registro de cada mudança de status do pedido (rastreabilidade e tempos de atendimento). */
@Entity
@Table(name = "historico_status_pedido")
public class HistoricoStatusPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    /** Nulo quando a mudança é feita pelo sistema ou pelo próprio cliente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Nulo no registro de criação do pedido. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusPedido statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusPedido statusNovo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected HistoricoStatusPedido() {
    }

    HistoricoStatusPedido(Pedido pedido, StatusPedido anterior, StatusPedido novo, Usuario usuario) {
        this.pedido = pedido;
        this.statusAnterior = anterior;
        this.statusNovo = novo;
        this.usuario = usuario;
        this.dataHora = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Usuario getUsuario() { return usuario; }
    public StatusPedido getStatusAnterior() { return statusAnterior; }
    public StatusPedido getStatusNovo() { return statusNovo; }
    public LocalDateTime getDataHora() { return dataHora; }
}

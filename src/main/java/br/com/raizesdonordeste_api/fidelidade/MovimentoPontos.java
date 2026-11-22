package br.com.raizesdonordeste_api.fidelidade;

import java.time.LocalDateTime;

import br.com.raizesdonordeste_api.cliente.Cliente;
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

/** Extrato de pontos do cliente: cada crédito, resgate ou estorno gera um registro. */
@Entity
@Table(name = "movimento_pontos")
public class MovimentoPontos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentoPontos tipo;

    @Column(nullable = false)
    private int pontos;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected MovimentoPontos() {
    }

    public MovimentoPontos(Cliente cliente, Pedido pedido, TipoMovimentoPontos tipo, int pontos) {
        this.cliente = cliente;
        this.pedido = pedido;
        this.tipo = tipo;
        this.pontos = pontos;
        this.dataHora = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Pedido getPedido() { return pedido; }
    public TipoMovimentoPontos getTipo() { return tipo; }
    public int getPontos() { return pontos; }
    public LocalDateTime getDataHora() { return dataHora; }
}

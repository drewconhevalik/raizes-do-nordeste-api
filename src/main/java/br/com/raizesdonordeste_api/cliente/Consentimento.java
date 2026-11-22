package br.com.raizesdonordeste_api.cliente;

import java.time.LocalDateTime;

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
 * Registro de uma decisão do titular sobre uma finalidade. Cada concessão ou revogação gera
 * um novo registro, preservando a prova do consentimento (LGPD).
 */
@Entity
@Table(name = "consentimento")
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Finalidade finalidade;

    @Column(nullable = false)
    private boolean concedido;

    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected Consentimento() {
    }

    public Consentimento(Cliente cliente, Finalidade finalidade, boolean concedido, String versaoTermo) {
        this.cliente = cliente;
        this.finalidade = finalidade;
        this.concedido = concedido;
        this.versaoTermo = versaoTermo;
        this.dataHora = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Finalidade getFinalidade() { return finalidade; }
    public boolean isConcedido() { return concedido; }
    public String getVersaoTermo() { return versaoTermo; }
    public LocalDateTime getDataHora() { return dataHora; }
}

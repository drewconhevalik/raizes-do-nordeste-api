package br.com.raizesdonordeste_api.cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Cliente identificado (participante do programa de fidelidade). */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    /** Opcional; usada apenas como faixa etária (RN12). */
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "saldo_pontos", nullable = false)
    private int saldoPontos;

    @Column(nullable = false)
    private boolean anonimizado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @OrderBy("dataHora ASC, id ASC")
    private List<Consentimento> consentimentos = new ArrayList<>();

    protected Cliente() {
    }

    public Cliente(String nome, String email, String telefone, String senhaHash, LocalDate dataNascimento) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.senhaHash = senhaHash;
        this.dataNascimento = dataNascimento;
        this.criadoEm = LocalDateTime.now();
    }

    /** Registra uma nova decisão do titular; o histórico anterior é preservado. */
    public void registrarConsentimento(Finalidade finalidade, boolean concedido, String versaoTermo) {
        consentimentos.add(new Consentimento(this, finalidade, concedido, versaoTermo));
    }

    /** Situação atual: vale a decisão mais recente para a finalidade. */
    public boolean possuiConsentimento(Finalidade finalidade) {
        boolean concedido = false;
        for (Consentimento c : consentimentos) {
            if (c.getFinalidade() == finalidade) {
                concedido = c.isConcedido();
            }
        }
        return concedido && !anonimizado;
    }

    public String faixaEtaria() {
        if (dataNascimento == null) {
            return "NAO_INFORMADA";
        }
        int idade = Period.between(dataNascimento, LocalDate.now()).getYears();
        if (idade < 25) return "18-24";
        if (idade < 35) return "25-34";
        if (idade < 45) return "35-44";
        if (idade < 60) return "45-59";
        return "60+";
    }

    public void creditarPontos(int pontos) {
        saldoPontos += pontos;
    }

    public void debitarPontos(int pontos) {
        if (pontos > saldoPontos) {
            throw new RegraNegocioException("Saldo de pontos insuficiente: disponível " + saldoPontos
                    + ", solicitado " + pontos + ".");
        }
        saldoPontos -= pontos;
    }

    /**
     * Eliminação a pedido do titular (RN18): remove os dados pessoais e mantém o registro
     * para que os pedidos continuem válidos para fins financeiros e estatísticos.
     */
    public void anonimizar() {
        this.nome = "Cliente anonimizado";
        this.email = "anonimo-" + id + "@anonimizado.invalid";
        this.telefone = null;
        this.dataNascimento = null;
        this.senhaHash = "!";
        this.saldoPontos = 0;
        this.anonimizado = true;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getSenhaHash() { return senhaHash; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public int getSaldoPontos() { return saldoPontos; }
    public boolean isAnonimizado() { return anonimizado; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<Consentimento> getConsentimentos() { return consentimentos; }
}

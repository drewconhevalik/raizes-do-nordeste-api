package br.com.raizesdonordeste_api.auditoria;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.usuario.Usuario;
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
import jakarta.persistence.Table;

/**
 * Registro de auditoria. É imutável (@Immutable, sem setters): depois de gravado não pode
 * ser alterado (RN16), e o serviço de auditoria não expõe exclusão.
 */
@Entity
@Immutable
@Table(name = "registro_auditoria", indexes = @Index(name = "idx_auditoria_data_acao", columnList = "data_hora, acao"))
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nulo quando a ação é do próprio cliente ou do sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    @Column(nullable = false, length = 50)
    private String entidade;

    @Column(name = "entidade_id")
    private Integer entidadeId;

    @Column(name = "valor_anterior", length = 2000)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 2000)
    private String valorNovo;

    @Column(length = 255)
    private String motivo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Usuario usuario, Unidade unidade, AcaoAuditoria acao, String entidade,
            Integer entidadeId, String valorAnterior, String valorNovo, String motivo) {
        this.usuario = usuario;
        this.unidade = unidade;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.motivo = motivo;
        this.dataHora = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Unidade getUnidade() { return unidade; }
    public AcaoAuditoria getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public Integer getEntidadeId() { return entidadeId; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNovo() { return valorNovo; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getDataHora() { return dataHora; }
}

package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.raizesdonordeste_api.unidade.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Produto disponibilizado no cardápio de uma unidade, com preço local, vigência
 * sazonal e observação de variação regional (RN01, RN02).
 */
@Entity
@Table(name = "item_cardapio",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unidade_id", "produto_id"}))
public class ItemCardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    /** Nulo significa usar o preço base do catálogo. */
    @Column(name = "preco_local", precision = 10, scale = 2)
    private BigDecimal precoLocal;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(name = "observacao_regional", length = 255)
    private String observacaoRegional;

    protected ItemCardapio() {
    }

    public ItemCardapio(Unidade unidade, Produto produto) {
        this.unidade = unidade;
        this.produto = produto;
    }

    /** RN02: preço local da unidade ou, na ausência dele, o preço base do catálogo. */
    public BigDecimal precoVigente() {
        return precoLocal != null ? precoLocal : produto.getPrecoBase();
    }

    /** Verifica se a data está dentro do período de vigência (produtos sazonais). */
    public boolean estaVigente(LocalDate data) {
        boolean depoisDoInicio = vigenciaInicio == null || !data.isBefore(vigenciaInicio);
        boolean antesDoFim = vigenciaFim == null || !data.isAfter(vigenciaFim);
        return depoisDoInicio && antesDoFim;
    }

    public Integer getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Produto getProduto() { return produto; }
    public BigDecimal getPrecoLocal() { return precoLocal; }
    public void setPrecoLocal(BigDecimal precoLocal) { this.precoLocal = precoLocal; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
    public LocalDate getVigenciaInicio() { return vigenciaInicio; }
    public void setVigenciaInicio(LocalDate vigenciaInicio) { this.vigenciaInicio = vigenciaInicio; }
    public LocalDate getVigenciaFim() { return vigenciaFim; }
    public void setVigenciaFim(LocalDate vigenciaFim) { this.vigenciaFim = vigenciaFim; }
    public String getObservacaoRegional() { return observacaoRegional; }
    public void setObservacaoRegional(String observacaoRegional) { this.observacaoRegional = observacaoRegional; }
}

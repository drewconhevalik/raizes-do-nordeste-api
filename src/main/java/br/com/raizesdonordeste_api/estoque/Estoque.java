package br.com.raizesdonordeste_api.estoque;

import br.com.raizesdonordeste_api.catalogo.Produto;
import br.com.raizesdonordeste_api.comum.RegraNegocioException;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;

/**
 * Saldo de um produto em uma unidade. O campo {@code version} implementa o controle de
 * concorrência otimista: se duas transações alterarem o mesmo saldo ao mesmo tempo,
 * apenas a primeira é gravada (RNF04).
 */
@Entity
@Table(name = "estoque",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unidade_id", "produto_id"}))
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Min(0)
    @Column(nullable = false)
    private int quantidade;

    @Version
    private Integer version;

    protected Estoque() {
    }

    public Estoque(Unidade unidade, Produto produto, int quantidade) {
        this.unidade = unidade;
        this.produto = produto;
        this.quantidade = quantidade;
    }

    /** Baixa do estoque; nunca deixa o saldo negativo (RN03). */
    public void baixar(int qtd) {
        if (qtd <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
        if (qtd > quantidade) {
            throw new RegraNegocioException("Estoque insuficiente para '" + produto.getNome()
                    + "': disponível " + quantidade + ", solicitado " + qtd + ".");
        }
        quantidade -= qtd;
    }

    public void repor(int qtd) {
        if (qtd <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
        quantidade += qtd;
    }

    /** Ajuste manual para um novo saldo (inventário); deve ser auditado pelo serviço. */
    public void ajustarPara(int novaQuantidade) {
        if (novaQuantidade < 0) {
            throw new RegraNegocioException("O estoque não pode ser negativo.");
        }
        quantidade = novaQuantidade;
    }

    public Integer getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public Integer getVersion() { return version; }
}

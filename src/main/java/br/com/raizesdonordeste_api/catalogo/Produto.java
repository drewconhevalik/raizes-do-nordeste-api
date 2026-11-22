package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Produto do catálogo global da franquia, mantido pela matriz (RNF11). */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaProduto categoria;

    @Column(name = "preco_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "exige_cozinha_completa", nullable = false)
    private boolean exigeCozinhaCompleta;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Produto() {
    }

    public Produto(String nome, String descricao, CategoriaProduto categoria, BigDecimal precoBase,
            boolean exigeCozinhaCompleta) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.precoBase = precoBase;
        this.exigeCozinhaCompleta = exigeCozinhaCompleta;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public CategoriaProduto getCategoria() { return categoria; }
    public void setCategoria(CategoriaProduto categoria) { this.categoria = categoria; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public void setPrecoBase(BigDecimal precoBase) { this.precoBase = precoBase; }
    public boolean isExigeCozinhaCompleta() { return exigeCozinhaCompleta; }
    public void setExigeCozinhaCompleta(boolean exigeCozinhaCompleta) { this.exigeCozinhaCompleta = exigeCozinhaCompleta; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}

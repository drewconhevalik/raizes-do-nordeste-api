package br.com.raizesdonordeste_api.unidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Regiao regiao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormatoUnidade formato;

    @Column(nullable = false)
    private boolean ativa = true;

    protected Unidade() {
    }

    public Unidade(String nome, String cidade, String uf, Regiao regiao, FormatoUnidade formato) {
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf;
        this.regiao = regiao;
        this.formato = formato;
    }

    public boolean possuiCozinhaCompleta() {
        return formato == FormatoUnidade.COMPLETA;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public Regiao getRegiao() { return regiao; }
    public void setRegiao(Regiao regiao) { this.regiao = regiao; }
    public FormatoUnidade getFormato() { return formato; }
    public void setFormato(FormatoUnidade formato) { this.formato = formato; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}

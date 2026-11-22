package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;

public record ProdutoResponse(
        Integer id,
        String nome,
        String descricao,
        CategoriaProduto categoria,
        BigDecimal precoBase,
        boolean exigeCozinhaCompleta,
        boolean ativo) {

    public static ProdutoResponse de(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getPrecoBase(),
                p.isExigeCozinhaCompleta(), p.isAtivo());
    }
}

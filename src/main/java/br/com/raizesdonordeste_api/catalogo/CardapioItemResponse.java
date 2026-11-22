package br.com.raizesdonordeste_api.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardapioItemResponse(
        Integer produtoId,
        String nome,
        String descricao,
        CategoriaProduto categoria,
        BigDecimal preco,
        String observacaoRegional,
        boolean disponivel,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim) {

    public static CardapioItemResponse de(ItemCardapio item) {
        Produto p = item.getProduto();
        return new CardapioItemResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(),
                item.precoVigente(), item.getObservacaoRegional(), item.isDisponivel(), item.getVigenciaInicio(),
                item.getVigenciaFim());
    }
}

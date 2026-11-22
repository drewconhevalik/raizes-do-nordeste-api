package br.com.raizesdonordeste_api.estoque;

public record EstoqueResponse(Integer produtoId, String produtoNome, int quantidade) {

    public static EstoqueResponse de(Estoque e) {
        return new EstoqueResponse(e.getProduto().getId(), e.getProduto().getNome(), e.getQuantidade());
    }
}

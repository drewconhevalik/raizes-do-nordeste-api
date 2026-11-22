package br.com.raizesdonordeste_api.unidade;

public record UnidadeResponse(
        Integer id,
        String nome,
        String cidade,
        String uf,
        Regiao regiao,
        FormatoUnidade formato,
        boolean ativa) {

    public static UnidadeResponse de(Unidade u) {
        return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.getUf(), u.getRegiao(), u.getFormato(),
                u.isAtiva());
    }
}

package br.com.raizesdonordeste_api.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioResponse(
        String relatorio,
        LocalDate inicio,
        LocalDate fim,
        String agrupamento,
        List<Linha> linhas,
        long quantidadeTotal,
        BigDecimal valorTotal) {

    /** Uma linha do relatório: o grupo (unidade, região ou produto), a quantidade e o valor. */
    public record Linha(String grupo, long quantidade, BigDecimal valor) {
    }
}

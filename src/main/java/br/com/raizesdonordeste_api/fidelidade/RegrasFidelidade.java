package br.com.raizesdonordeste_api.fidelidade;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Parâmetros do programa de fidelidade (premissas RN08 e RN09). */
public final class RegrasFidelidade {

    /** RN08: 1 ponto por real inteiro pago. */
    public static final int PONTOS_POR_REAL = 1;
    /** RN09: cada bloco de 100 pontos vale R$ 5,00 de desconto. */
    public static final int PONTOS_POR_BLOCO = 100;
    public static final BigDecimal VALOR_POR_BLOCO = new BigDecimal("5.00");
    /** RN09: o resgate cobre no máximo 50% do valor dos itens. */
    public static final BigDecimal LIMITE_RESGATE = new BigDecimal("0.50");

    private RegrasFidelidade() {
    }

    public static BigDecimal valorDoResgate(int pontos) {
        return VALOR_POR_BLOCO.multiply(BigDecimal.valueOf(pontos / PONTOS_POR_BLOCO)).setScale(2, RoundingMode.HALF_UP);
    }

    public static int pontosGanhos(BigDecimal valorPago) {
        return valorPago.setScale(0, RoundingMode.DOWN).intValue() * PONTOS_POR_REAL;
    }
}

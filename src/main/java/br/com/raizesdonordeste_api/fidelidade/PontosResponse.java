package br.com.raizesdonordeste_api.fidelidade;

import java.time.LocalDateTime;
import java.util.List;

public record PontosResponse(int saldo, List<Movimento> extrato) {

    public record Movimento(TipoMovimentoPontos tipo, int pontos, Integer pedidoId, LocalDateTime dataHora) {
    }
}

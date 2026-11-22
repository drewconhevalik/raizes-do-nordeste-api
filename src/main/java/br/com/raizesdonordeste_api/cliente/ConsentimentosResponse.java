package br.com.raizesdonordeste_api.cliente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Situação atual por finalidade e o histórico completo de decisões do titular. */
public record ConsentimentosResponse(Map<Finalidade, Boolean> situacaoAtual, List<Registro> historico) {

    public record Registro(Finalidade finalidade, boolean concedido, String versaoTermo, LocalDateTime dataHora) {
    }
}

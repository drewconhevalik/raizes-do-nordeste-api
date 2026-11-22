package br.com.raizesdonordeste_api.auditoria;

import java.time.LocalDateTime;

public record RegistroAuditoriaResponse(
        Integer id,
        LocalDateTime dataHora,
        AcaoAuditoria acao,
        String entidade,
        Integer entidadeId,
        String valorAnterior,
        String valorNovo,
        String motivo,
        String autor,
        Integer unidadeId) {

    public static RegistroAuditoriaResponse de(RegistroAuditoria r) {
        String autor = r.getUsuario() != null ? r.getUsuario().getNome() : "sistema ou titular dos dados";
        return new RegistroAuditoriaResponse(r.getId(), r.getDataHora(), r.getAcao(), r.getEntidade(),
                r.getEntidadeId(), r.getValorAnterior(), r.getValorNovo(), r.getMotivo(), autor,
                r.getUnidade() != null ? r.getUnidade().getId() : null);
    }
}

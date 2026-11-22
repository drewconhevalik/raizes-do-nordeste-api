package br.com.raizesdonordeste_api.auditoria;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Integer> {
    long countByAcao(AcaoAuditoria acao);

    long countByAcaoAndEntidadeId(AcaoAuditoria acao, Integer entidadeId);

    /** Usa o índice (data_hora, acao). */
    List<RegistroAuditoria> findTop500ByDataHoraBetweenOrderByDataHoraDesc(LocalDateTime inicio, LocalDateTime fim);
}

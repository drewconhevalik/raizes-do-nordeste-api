package br.com.raizesdonordeste_api.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Integer> {
    long countByAcao(AcaoAuditoria acao);

    long countByAcaoAndEntidadeId(AcaoAuditoria acao, Integer entidadeId);
}

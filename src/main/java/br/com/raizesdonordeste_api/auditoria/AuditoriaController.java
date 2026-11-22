package br.com.raizesdonordeste_api.auditoria;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasRole('ADMIN_MATRIZ')")
@Tag(name = "Auditoria", description = "Trilha de auditoria de operações sensíveis (RF21)")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta a trilha de auditoria (padrão: últimos 30 dias; máximo de 500 registros)")
    public List<RegistroAuditoriaResponse> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(required = false) Integer unidadeId) {
        return service.consultar(inicio, fim, acao, unidadeId);
    }
}

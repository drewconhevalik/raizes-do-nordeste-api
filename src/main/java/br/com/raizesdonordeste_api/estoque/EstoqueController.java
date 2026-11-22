package br.com.raizesdonordeste_api.estoque;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/unidades/{unidadeId}/estoque")
@PreAuthorize("hasAnyRole('GERENTE','ADMIN_MATRIZ')")
@Tag(name = "Estoque", description = "Estoque local por unidade (RF07)")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Saldo de estoque da unidade")
    public List<EstoqueResponse> listar(@PathVariable Integer unidadeId) {
        return service.listar(unidadeId);
    }

    @PostMapping("/{produtoId}/entradas")
    @Operation(summary = "Registra entrada de mercadoria")
    public EstoqueResponse entrada(@PathVariable Integer unidadeId, @PathVariable Integer produtoId,
            @Valid @RequestBody EntradaEstoqueRequest requisicao) {
        return service.registrarEntrada(unidadeId, produtoId, requisicao);
    }

    @PostMapping("/{produtoId}/ajustes")
    @Operation(summary = "Ajuste manual de saldo com motivo (auditado)")
    public EstoqueResponse ajuste(@PathVariable Integer unidadeId, @PathVariable Integer produtoId,
            @Valid @RequestBody AjusteEstoqueRequest requisicao) {
        return service.ajustar(unidadeId, produtoId, requisicao);
    }
}

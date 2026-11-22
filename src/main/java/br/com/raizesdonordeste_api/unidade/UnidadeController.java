package br.com.raizesdonordeste_api.unidade;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/unidades")
@Tag(name = "Unidades", description = "Unidades da rede (RF03)")
public class UnidadeController {

    private final UnidadeService service;

    public UnidadeController(UnidadeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as unidades ativas (público)")
    public List<UnidadeResponse> listar() {
        return service.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    @Operation(summary = "Cria uma unidade (matriz)")
    public UnidadeResponse criar(@Valid @RequestBody UnidadeRequest requisicao) {
        return service.criar(requisicao);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    @Operation(summary = "Altera uma unidade (matriz)")
    public UnidadeResponse atualizar(@PathVariable Integer id, @Valid @RequestBody UnidadeRequest requisicao) {
        return service.atualizar(id, requisicao);
    }
}

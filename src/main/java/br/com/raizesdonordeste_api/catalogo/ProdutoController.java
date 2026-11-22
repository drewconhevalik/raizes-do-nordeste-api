package br.com.raizesdonordeste_api.catalogo;

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
@RequestMapping("/api/v1/produtos")
@Tag(name = "Catálogo", description = "Catálogo global de produtos da franquia (RF04)")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_MATRIZ','GERENTE')")
    @Operation(summary = "Lista o catálogo global")
    public List<ProdutoResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    @Operation(summary = "Cria produto no catálogo (matriz)")
    public ProdutoResponse criar(@Valid @RequestBody ProdutoRequest requisicao) {
        return service.criar(requisicao);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    @Operation(summary = "Altera produto do catálogo (matriz); mudança de preço é auditada")
    public ProdutoResponse atualizar(@PathVariable Integer id, @Valid @RequestBody ProdutoRequest requisicao) {
        return service.atualizar(id, requisicao);
    }
}

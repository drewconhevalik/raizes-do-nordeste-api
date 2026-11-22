package br.com.raizesdonordeste_api.catalogo;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/unidades/{unidadeId}/cardapio")
@Tag(name = "Cardápio", description = "Cardápio por unidade (RF05, RF06)")
public class CardapioController {

    private final CardapioService service;

    public CardapioController(CardapioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Cardápio disponível da unidade (público): apenas itens que atendem à RN01")
    public List<CardapioItemResponse> disponivel(@PathVariable Integer unidadeId) {
        return service.cardapioDisponivel(unidadeId);
    }

    @GetMapping("/gestao")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN_MATRIZ')")
    @Operation(summary = "Todos os itens configurados no cardápio da unidade, inclusive indisponíveis")
    public List<CardapioItemResponse> completo(@PathVariable Integer unidadeId) {
        return service.cardapioCompleto(unidadeId);
    }

    @PutMapping("/{produtoId}")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN_MATRIZ')")
    @Operation(summary = "Inclui ou altera produto no cardápio da unidade (preço local, vigência, disponibilidade)")
    public CardapioItemResponse configurar(@PathVariable Integer unidadeId, @PathVariable Integer produtoId,
            @Valid @RequestBody CardapioItemRequest requisicao) {
        return service.configurar(unidadeId, produtoId, requisicao);
    }
}

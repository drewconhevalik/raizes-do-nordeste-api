package br.com.raizesdonordeste_api.relatorio;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/relatorios")
@PreAuthorize("hasAnyRole('GERENTE','ADMIN_MATRIZ')")
@Tag(name = "Relatórios", description = "Relatórios gerenciais para a matriz e as unidades (RF22, RF23)")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/vendas")
    @Operation(summary = "Vendas pagas no período, por UNIDADE ou REGIAO (padrão: últimos 30 dias)")
    public RelatorioResponse vendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String agrupamento,
            @RequestParam(required = false) Integer unidadeId) {
        return service.vendas(inicio, fim, agrupamento, unidadeId);
    }

    @GetMapping("/produtos-mais-vendidos")
    @Operation(summary = "Ranking de produtos mais vendidos no período")
    public RelatorioResponse maisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Integer unidadeId,
            @RequestParam(defaultValue = "10") int limite) {
        return service.produtosMaisVendidos(inicio, fim, unidadeId, limite);
    }
}

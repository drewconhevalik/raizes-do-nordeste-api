package br.com.raizesdonordeste_api.cliente;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdonordeste_api.fidelidade.FidelidadeService;
import br.com.raizesdonordeste_api.fidelidade.PontosResponse;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes e LGPD", description = "Cadastro, consentimentos e direitos do titular (RF02, RF15, RF19)")
public class ClienteController {

    private final ClienteService service;
    private final FidelidadeService fidelidadeService;

    public ClienteController(ClienteService service, FidelidadeService fidelidadeService) {
        this.service = service;
        this.fidelidadeService = fidelidadeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro no programa de fidelidade com consentimentos por finalidade (público)")
    public ClienteResponse cadastrar(@Valid @RequestBody CadastroClienteRequest requisicao) {
        return service.cadastrar(requisicao);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Dados pessoais do próprio titular (acesso auditado)")
    public ClienteResponse meusDados() {
        return service.meusDados();
    }

    @GetMapping("/me/consentimentos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Situação atual e histórico dos consentimentos")
    public ConsentimentosResponse consentimentos() {
        return service.consentimentos();
    }

    @PutMapping("/me/consentimentos/{finalidade}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Concede ou revoga o consentimento de uma finalidade (FIDELIDADE ou MARKETING)")
    public ConsentimentosResponse alterarConsentimento(@PathVariable Finalidade finalidade,
            @Valid @RequestBody ConsentimentoRequest requisicao) {
        return service.alterarConsentimento(finalidade, requisicao.concedido());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Elimina (anonimiza) os dados pessoais do titular")
    public void eliminar() {
        service.eliminar();
    }

    @GetMapping("/me/pontos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Saldo e extrato de pontos de fidelidade")
    public PontosResponse pontos() {
        return fidelidadeService.extrato(ControleAcesso.exigirAutenticado().id());
    }
}

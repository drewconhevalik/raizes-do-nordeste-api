package br.com.raizesdonordeste_api.pagamento;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdonordeste_api.comum.CredenciaisInvalidasException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Pagamentos", description = "Integração com o serviço externo de pagamento (RF11, RF12)")
public class PagamentoController {

    private final PagamentoService service;
    private final byte[] tokenWebhook;

    public PagamentoController(PagamentoService service, @Value("${app.gateway.token-webhook}") String tokenWebhook) {
        this.service = service;
        this.tokenWebhook = tokenWebhook.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/pedidos/{id}/pagamentos")
    @Operation(summary = "Solicita o pagamento do pedido; exige o cabeçalho Idempotency-Key (8 a 100 caracteres)")
    public ResponseEntity<PagamentoResponse> pagar(@PathVariable Integer id,
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9_.:-]{8,100}") String chave,
            @Valid @RequestBody PagamentoRequest requisicao) {
        PagamentoService.Resultado resultado = service.iniciar(id, requisicao.forma(), chave);
        HttpStatus status = resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(resultado.pagamento());
    }

    @GetMapping("/pedidos/{id}/pagamentos")
    @Operation(summary = "Lista as tentativas de pagamento do pedido")
    public List<PagamentoResponse> listar(@PathVariable Integer id) {
        return service.listar(id);
    }

    @PostMapping("/pagamentos/webhook")
    @Operation(summary = "Confirmação assíncrona do serviço de pagamento (exige X-Gateway-Token)")
    public PagamentoResponse webhook(@RequestHeader(value = "X-Gateway-Token", required = false) String token,
            @Valid @RequestBody WebhookRequest notificacao) {
        if (token == null || !MessageDigest.isEqual(tokenWebhook, token.getBytes(StandardCharsets.UTF_8))) {
            throw new CredenciaisInvalidasException();
        }
        return service.processarWebhook(notificacao);
    }
}

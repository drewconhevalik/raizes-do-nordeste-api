package br.com.raizesdonordeste_api.pedido;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Pedidos", description = "Pedidos multicanal, fila e ciclo de status (RF08–RF10, RF13, RF14)")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria pedido (totem sem login; app com login de cliente; balcão com login da equipe)")
    public PedidoResponse criar(@Valid @RequestBody CriarPedidoRequest requisicao) {
        return service.criar(requisicao);
    }

    @GetMapping("/pedidos/{id}")
    @Operation(summary = "Consulta pedido (cliente dono ou equipe da unidade)")
    public PedidoResponse consultar(@PathVariable Integer id) {
        return service.consultar(id);
    }

    @GetMapping("/pedidos/acompanhamento/{codigo}")
    @Operation(summary = "Acompanhamento público pelo código do pedido")
    public PedidoResponse acompanhar(@PathVariable String codigo) {
        return service.acompanhar(codigo);
    }

    @GetMapping("/unidades/{unidadeId}/pedidos")
    @PreAuthorize("hasAnyRole('ATENDENTE','COZINHA','GERENTE','ADMIN_MATRIZ')")
    @Operation(summary = "Fila de pedidos da unidade (padrão: pagos, em preparo e prontos)")
    public List<PedidoResponse> fila(@PathVariable Integer unidadeId,
            @RequestParam(required = false) StatusPedido status) {
        return service.fila(unidadeId, status);
    }

    @PatchMapping("/pedidos/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE','COZINHA','GERENTE','ADMIN_MATRIZ')")
    @Operation(summary = "Avança o status operacional do pedido")
    public PedidoResponse alterarStatus(@PathVariable Integer id, @Valid @RequestBody AlterarStatusRequest requisicao) {
        return service.alterarStatus(id, requisicao.status());
    }

    @PostMapping("/pedidos/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE','ADMIN_MATRIZ')")
    @Operation(summary = "Cancela pedido com motivo (auditado); devolve estoque")
    public PedidoResponse cancelar(@PathVariable Integer id, @Valid @RequestBody CancelamentoRequest requisicao) {
        return service.cancelar(id, requisicao.motivo());
    }

    @PostMapping("/pedidos/{id}/desconto")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN_MATRIZ')")
    @Operation(summary = "Aplica desconto manual com motivo, até 20% (auditado)")
    public PedidoResponse desconto(@PathVariable Integer id, @Valid @RequestBody DescontoRequest requisicao) {
        return service.aplicarDesconto(id, requisicao);
    }
}

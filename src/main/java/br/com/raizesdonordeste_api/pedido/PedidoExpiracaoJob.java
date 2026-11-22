package br.com.raizesdonordeste_api.pedido;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Verifica a cada minuto os pedidos sem pagamento há mais de 15 minutos (RN04). */
@Component
public class PedidoExpiracaoJob {

    static final int MINUTOS_PARA_EXPIRAR = 15;
    private static final Logger log = LoggerFactory.getLogger(PedidoExpiracaoJob.class);

    private final PedidoService pedidoService;

    public PedidoExpiracaoJob(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void executar() {
        int expirados = pedidoService.expirarPendentes(LocalDateTime.now().minusMinutes(MINUTOS_PARA_EXPIRAR));
        if (expirados > 0) {
            log.info("{} pedido(s) expirado(s) por falta de pagamento.", expirados);
        }
    }
}

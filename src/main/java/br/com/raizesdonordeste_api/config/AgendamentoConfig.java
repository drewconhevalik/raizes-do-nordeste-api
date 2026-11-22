package br.com.raizesdonordeste_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita tarefas agendadas (expiração de pedidos não pagos, RN04). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}

package br.com.raizesdonordeste_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import br.com.raizesdonordeste_api.estoque.Estoque;

/**
 * Comportamento sob acesso simultâneo (RNF04) e tempo de resposta indicativo (RNF06).
 * Os testes rodam dentro do processo (sem rede); servem como verificação de regressão e
 * indício de desempenho, não substituem o teste de carga planejado com JMeter.
 */
@SpringBootTest
class ConcorrenciaDesempenhoTest extends ApiTestBase {

    private void definirSaldo(Integer unidadeId, Integer produtoId, int quantidade) {
        Estoque estoque = estoqueRepository.buscar(unidadeId, produtoId).orElseThrow();
        estoque.ajustarPara(quantidade);
        estoqueRepository.save(estoque);
    }

    @Test
    @DisplayName("CT-CON-01: 10 pedidos simultâneos pelo último item — só um é aceito e o estoque não fica negativo")
    void ultimoItemConcorrente() throws Exception {
        Integer recife = unidadeId("Raízes Boa Viagem");
        Integer produto = produtoId("Tapioca de carne de sol");
        definirSaldo(recife, produto, 1);
        String corpo = pedidoJson(recife, "TOTEM", produto, 1);

        int clientes = 10;
        ExecutorService pool = Executors.newFixedThreadPool(clientes);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (int i = 0; i < clientes; i++) {
            respostas.add(pool.submit(() -> {
                largada.await();
                return mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                        .andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown(); // todos disparam ao mesmo tempo
        List<Integer> status = new ArrayList<>();
        for (Future<Integer> resposta : respostas) {
            status.add(resposta.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        assertThat(status).filteredOn(s -> s != 201).allMatch(s -> s == 409 || s == 422);
        assertThat(saldo(recife, produto)).isZero();
        definirSaldo(recife, produto, 50); // restaura para os demais testes
    }

    @Test
    @DisplayName("CT-DES-01: consulta de cardápio com 50 acessos simultâneos — p95 abaixo de 300 ms")
    void desempenhoCardapio() throws Exception {
        Integer recife = unidadeId("Raízes Boa Viagem");
        int usuarios = 50;
        int requisicoesPorUsuario = 20;
        List<Long> tempos = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(usuarios);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> tarefas = new ArrayList<>();
        for (int u = 0; u < usuarios; u++) {
            tarefas.add(pool.submit(() -> {
                largada.await();
                for (int r = 0; r < requisicoesPorUsuario; r++) {
                    long inicio = System.nanoTime();
                    int codigo = mvc.perform(get("/api/v1/unidades/" + recife + "/cardapio"))
                            .andReturn().getResponse().getStatus();
                    tempos.add((System.nanoTime() - inicio) / 1_000_000);
                    assertThat(codigo).isEqualTo(200);
                }
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> tarefa : tarefas) {
            tarefa.get(120, TimeUnit.SECONDS);
        }
        pool.shutdown();

        List<Long> ordenados = new ArrayList<>(tempos);
        Collections.sort(ordenados);
        long p95 = ordenados.get((int) Math.ceil(ordenados.size() * 0.95) - 1);
        System.out.printf("CT-DES-01: %d requisições, p50=%d ms, p95=%d ms, máx=%d ms%n", ordenados.size(),
                ordenados.get(ordenados.size() / 2), p95, ordenados.get(ordenados.size() - 1));
        assertThat(ordenados).hasSize(usuarios * requisicoesPorUsuario);
        assertThat(p95).isLessThan(300);
    }
}

package br.com.raizesdonordeste_api.pagamento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Integer> {
    Optional<Pagamento> findByChaveIdempotencia(String chaveIdempotencia);

    Optional<Pagamento> findByIdTransacaoExterna(String idTransacaoExterna);

    List<Pagamento> findByPedidoIdOrderByIdAsc(Integer pedidoId);
}

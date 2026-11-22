package br.com.raizesdonordeste_api.pedido;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    Optional<Pedido> findByCodigoAcompanhamento(String codigoAcompanhamento);

    /** Fila da unidade em ordem de chegada (usa o índice unidade_id + status). */
    @Query("select p from Pedido p where p.unidade.id = :unidadeId and p.status in :status order by p.criadoEm asc")
    List<Pedido> fila(@Param("unidadeId") Integer unidadeId, @Param("status") Collection<StatusPedido> status);
}

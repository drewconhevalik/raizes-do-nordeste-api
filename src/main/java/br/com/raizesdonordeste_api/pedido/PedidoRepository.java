package br.com.raizesdonordeste_api.pedido;

import java.time.LocalDateTime;
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

    /** Pedidos aguardando pagamento criados antes do limite (expiração, RN04). */
    List<Pedido> findByStatusAndCriadoEmBefore(StatusPedido status, LocalDateTime limite);

    @Query("select p.unidade.nome, count(p), sum(p.valorTotal) from Pedido p "
            + "where p.status in :status and p.criadoEm >= :inicio and p.criadoEm < :fim "
            + "group by p.unidade.nome order by sum(p.valorTotal) desc")
    List<Object[]> vendasPorUnidade(@Param("status") Collection<StatusPedido> status,
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select p.unidade.nome, count(p), sum(p.valorTotal) from Pedido p "
            + "where p.status in :status and p.criadoEm >= :inicio and p.criadoEm < :fim and p.unidade.id = :unidadeId "
            + "group by p.unidade.nome")
    List<Object[]> vendasDaUnidade(@Param("status") Collection<StatusPedido> status,
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
            @Param("unidadeId") Integer unidadeId);

    @Query("select p.unidade.regiao, count(p), sum(p.valorTotal) from Pedido p "
            + "where p.status in :status and p.criadoEm >= :inicio and p.criadoEm < :fim "
            + "group by p.unidade.regiao order by sum(p.valorTotal) desc")
    List<Object[]> vendasPorRegiao(@Param("status") Collection<StatusPedido> status,
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}

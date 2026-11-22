package br.com.raizesdonordeste_api.pedido;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Integer> {

    @Query("select i.produto.nome, sum(i.quantidade), sum(i.subtotal) from ItemPedido i "
            + "where i.pedido.status in :status and i.pedido.criadoEm >= :inicio and i.pedido.criadoEm < :fim "
            + "group by i.produto.nome order by sum(i.quantidade) desc")
    List<Object[]> maisVendidos(@Param("status") Collection<StatusPedido> status,
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, Pageable pagina);

    @Query("select i.produto.nome, sum(i.quantidade), sum(i.subtotal) from ItemPedido i "
            + "where i.pedido.status in :status and i.pedido.criadoEm >= :inicio and i.pedido.criadoEm < :fim "
            + "and i.pedido.unidade.id = :unidadeId "
            + "group by i.produto.nome order by sum(i.quantidade) desc")
    List<Object[]> maisVendidosDaUnidade(@Param("status") Collection<StatusPedido> status,
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
            @Param("unidadeId") Integer unidadeId, Pageable pagina);
}

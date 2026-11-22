package br.com.raizesdonordeste_api.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapio, Integer> {

    @Query("select i from ItemCardapio i join fetch i.produto join fetch i.unidade "
            + "where i.unidade.id = :unidadeId order by i.produto.nome")
    List<ItemCardapio> listarPorUnidade(@Param("unidadeId") Integer unidadeId);

    @Query("select i from ItemCardapio i join fetch i.produto join fetch i.unidade "
            + "where i.unidade.id = :unidadeId and i.produto.id = :produtoId")
    Optional<ItemCardapio> buscar(@Param("unidadeId") Integer unidadeId, @Param("produtoId") Integer produtoId);
}

package br.com.raizesdonordeste_api.estoque;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueRepository extends JpaRepository<Estoque, Integer> {

    @Query("select e from Estoque e join fetch e.produto where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
    Optional<Estoque> buscar(@Param("unidadeId") Integer unidadeId, @Param("produtoId") Integer produtoId);

    @Query("select e from Estoque e join fetch e.produto where e.unidade.id = :unidadeId order by e.produto.nome")
    List<Estoque> listarPorUnidade(@Param("unidadeId") Integer unidadeId);
}

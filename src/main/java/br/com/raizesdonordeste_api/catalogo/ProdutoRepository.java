package br.com.raizesdonordeste_api.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {
    List<Produto> findAllByOrderByNome();
}

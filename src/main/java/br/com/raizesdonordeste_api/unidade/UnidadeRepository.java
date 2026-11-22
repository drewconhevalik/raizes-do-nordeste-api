package br.com.raizesdonordeste_api.unidade;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {
    List<Unidade> findByAtivaTrueOrderByNome();
}

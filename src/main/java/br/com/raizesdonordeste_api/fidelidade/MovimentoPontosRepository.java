package br.com.raizesdonordeste_api.fidelidade;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoPontosRepository extends JpaRepository<MovimentoPontos, Integer> {
    List<MovimentoPontos> findByClienteIdOrderByDataHoraDesc(Integer clienteId);
}

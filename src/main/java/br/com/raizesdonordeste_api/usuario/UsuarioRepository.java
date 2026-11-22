package br.com.raizesdonordeste_api.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Query("select u from Usuario u left join fetch u.unidade where u.email = :email")
    Optional<Usuario> buscarPorEmail(@Param("email") String email);
}

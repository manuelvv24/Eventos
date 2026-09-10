package Eventos.eventos.repository;

import Eventos.eventos.entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {
    boolean existsByEmailUsuario(String emailUsuario);
    Optional<Login> findByEmailUsuario(String emailUsuario);
    Optional<Login> findByEmailRecuperacion(String emailRecuperacion);
    Optional<Login> findByUsuario_IdUsuario(Long idUsuario);
}
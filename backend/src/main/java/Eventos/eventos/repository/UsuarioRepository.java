package Eventos.eventos.repository;

import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByNumeroDocumentoUsuario(String numeroDocumentoUsuario);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u JOIN u.roles r WHERE r = :rol")
    boolean existsByRol(@Param("rol") Rol rol);
}
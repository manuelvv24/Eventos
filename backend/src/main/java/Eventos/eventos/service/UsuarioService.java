package Eventos.eventos.service;

import Eventos.eventos.dto.UsuarioDTO;
import java.util.List;

public interface UsuarioService {
    List<UsuarioDTO> listarTodos();
    List<UsuarioDTO> listarEliminados();
    UsuarioDTO obtenerPorId(Long id);
    UsuarioDTO registrarUsuario(UsuarioDTO usuarioDTO);
    UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO);
    void eliminarUsuario(Long id);
    UsuarioDTO restaurarUsuario(Long id);
}
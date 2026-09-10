package Eventos.eventos.service;

import Eventos.eventos.dto.RegistroAsistenciaDTO;
import java.util.List;

public interface RegistroAsistenciaService {
    List<RegistroAsistenciaDTO> obtenerTodos();
    List<RegistroAsistenciaDTO> listarEliminados();
    List<RegistroAsistenciaDTO> obtenerPorEvento(Long idEvento);
    RegistroAsistenciaDTO registrarAsistencia(RegistroAsistenciaDTO dto);
    RegistroAsistenciaDTO restaurar(Long id);
    void eliminar(Long id);
}
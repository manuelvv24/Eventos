package Eventos.eventos.service;

import Eventos.eventos.dto.EventoDTO;
import java.util.List;

public interface EventoService {
    List<EventoDTO> obtenerTodos();
    EventoDTO obtenerPorId(Long id);
    EventoDTO crear(EventoDTO dto);
    EventoDTO actualizar(Long id, EventoDTO dto);
    EventoDTO cancelarEvento(Long id);
    void eliminarLogico(Long id);
    EventoDTO restaurar(Long id);
    List<EventoDTO> obtenerEliminados();
}
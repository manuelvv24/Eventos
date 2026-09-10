package Eventos.eventos.service;

import Eventos.eventos.dto.ParticipanteDto;
import java.util.List;

public interface ParticipanteService {
    ParticipanteDto registrar(ParticipanteDto dto);
    List<ParticipanteDto> obtenerTodos();
    ParticipanteDto obtenerPorId(Long id);
}
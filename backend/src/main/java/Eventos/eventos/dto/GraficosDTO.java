package Eventos.eventos.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GraficosDTO {
    private List<String> nombresEventos;
    private List<Long> asistenciaPorEvento;
    private Map<String, Long> eventosPorModalidad;
    private Map<String, Long> eventosPorTipo;
    private List<String> meses;
    private List<Long> registrosPorMes;
}
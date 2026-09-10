package Eventos.eventos.service;

import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Notificacion;
import Eventos.eventos.entity.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificacionService {
    Notificacion enviar(Usuario usuario, Evento evento, String tipo, String asunto, String descripcion);
    Notificacion programar(Usuario usuario, Evento evento, String tipo, String asunto, String descripcion, LocalDateTime fechaProgramada);
    void programarRecordatorio(Usuario usuario, Evento evento);
    void notificarInscritos(Evento evento, String tipo, String asunto, String descripcion);
    void procesarProgramadas();
    List<Map<String, Object>> obtenerDeUsuario(Long idUsuario);
    long contarNoLeidas(Long idUsuario);
    void marcarLeida(Long idNotificacion, Long idUsuario);
    void marcarTodasLeidas(Long idUsuario);
}
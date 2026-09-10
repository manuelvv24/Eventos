package Eventos.eventos.scheduler;

import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Estado;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.service.EstadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventoScheduler {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private EstadoService estadoService;

    @Scheduled(fixedRate = 60000) // Revisa cada 60 segundos
    @Transactional
    public void finalizarEventosVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Evento> todos = eventoRepository.findAll();

        for (Evento evento : todos) {
            String estadoActual = evento.getEstado() != null ? evento.getEstado().getNombreEstado() : null;
            if ("FINALIZADO".equalsIgnoreCase(estadoActual) ||
                "CANCELADO".equalsIgnoreCase(estadoActual)) {
                continue;
            }

            LocalDateTime fechaFin = evento.getFechaFinEvento() != null ?
                    evento.getFechaFinEvento() : evento.getFechaInicioEvento();

            if (fechaFin != null && fechaFin.isBefore(ahora)) {
                Estado estadoFinalizado = estadoService.resolver(EstadoService.TIPO_EVENTO, "FINALIZADO");
                evento.setEstado(estadoFinalizado);
                eventoRepository.save(evento);
                System.out.println("[EVENTO FINALIZADO AUTO] ID=" + evento.getIdEventos() + " Nombre=" + evento.getNombreEvento());
            }
        }
    }
}

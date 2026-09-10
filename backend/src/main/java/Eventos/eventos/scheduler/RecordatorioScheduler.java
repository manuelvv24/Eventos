package Eventos.eventos.scheduler;

import Eventos.eventos.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecordatorioScheduler {

    @Autowired private NotificacionService notificacionService;

    @Scheduled(fixedRate = 60000)
    public void procesarNotificacionesProgramadas() {
        notificacionService.procesarProgramadas();
    }
}
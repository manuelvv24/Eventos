package Eventos.eventos.audit;

import Eventos.eventos.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditEventHandler {

    @Autowired
    private AuditoriaService auditoriaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(EventoAuditoria e) {
        try {
            auditoriaService.registrar(e.accion(), e.entidad(), e.idEntidad(),
                    e.detalle(), e.idUsuario(), e.ip());
        } catch (Exception ex) {
            System.err.println("[AUDIT ERROR] " + ex.getMessage());
        }
    }
}
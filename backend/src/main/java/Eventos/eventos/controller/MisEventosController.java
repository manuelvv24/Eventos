package Eventos.eventos.controller;

import Eventos.eventos.audit.SeguridadUtils;
import Eventos.eventos.dto.MiEventoDTO;
import Eventos.eventos.service.MisEventosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitado")
public class MisEventosController {

    @Autowired
    private MisEventosService misEventosService;

    @GetMapping("/mis-eventos")
    public ResponseEntity<List<MiEventoDTO>> misEventos() {
        Long idUsuario = SeguridadUtils.idUsuarioActual();
        if (idUsuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(misEventosService.misEventos(idUsuario));
    }
}
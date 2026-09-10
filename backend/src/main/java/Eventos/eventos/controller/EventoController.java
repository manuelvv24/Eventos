package Eventos.eventos.controller;

import Eventos.eventos.dto.EventoDTO;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.CloudinaryService;
import Eventos.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "*")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private RegistroAsistenciaRepository asistenciaRepository;

    @GetMapping
    public ResponseEntity<List<EventoDTO>> listarTodos() {
        return ResponseEntity.ok(eventoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/cupos")
    public ResponseEntity<?> cuposDisponibles(@PathVariable Long id) {
        return eventoRepository.findById(id)
            .filter(e -> !Boolean.TRUE.equals(e.getEliminado()))
            .map(evento -> {
            long registrados = asistenciaRepository.findByEvento_IdEventos(id).size();
            Integer aforo    = evento.getAforoMaximoEvento();
            long disponibles = aforo != null ? Math.max(0, aforo - registrados) : -1;
            return ResponseEntity.ok(Map.of(
                "idEvento",     id,
                "aforoMaximo",  aforo != null ? aforo : 0,
                "registrados",  registrados,
                "disponibles",  disponibles,
                "sinLimite",    aforo == null
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EventoDTO> crear(@Valid @RequestBody EventoDTO dto) {
        return new ResponseEntity<>(eventoService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody EventoDTO dto) {
        return ResponseEntity.ok(eventoService.actualizar(id, dto));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<EventoDTO> cancelarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.cancelarEvento(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        eventoService.eliminarLogico(id);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Evento eliminado correctamente",
            "idEvento", id.toString()
        ));
    }

    @PutMapping("/{id}/restaurar")
    public ResponseEntity<EventoDTO> restaurar(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.restaurar(id));
    }

    @GetMapping("/papelera")
    public ResponseEntity<List<EventoDTO>> listarEliminados() {
        return ResponseEntity.ok(eventoService.obtenerEliminados());
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirImagen(
            @PathVariable Long id,
            @RequestParam("imagen") MultipartFile imagen) {
        try {
            String urlImagen = cloudinaryService.subirImagen(imagen, "eventos");
            EventoDTO dto = eventoService.obtenerPorId(id);
            dto.setImagenUrl(urlImagen);
            eventoService.actualizar(id, dto);
            return ResponseEntity.ok(Map.of(
                    "imagenUrl", urlImagen,
                    "mensaje",   "Imagen subida correctamente"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
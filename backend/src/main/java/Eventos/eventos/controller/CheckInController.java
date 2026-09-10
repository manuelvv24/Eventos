package Eventos.eventos.controller;

import Eventos.eventos.dto.CheckInDTO;
import Eventos.eventos.dto.CheckInMasivoDTO;
import Eventos.eventos.service.CheckInService;
import Eventos.eventos.service.impl.CheckInServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/check-in")
public class CheckInController {

    @Autowired
    private CheckInServiceImpl checkInService;

    @PostMapping
    public ResponseEntity<?> marcarAsistencia(@Valid @RequestBody CheckInDTO dto,
                                              HttpServletRequest request) {
        try {
            CheckInDTO resultado = checkInService.realizarCheckIn(dto);
            return new ResponseEntity<>(resultado, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/qr")
    public ResponseEntity<?> checkInPorQr(@RequestBody Map<String, String> body,
                                          Authentication auth,
                                          HttpServletRequest request) {
        try {
            String codigoQr = body.get("codigoQr");
            if (codigoQr == null || codigoQr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'codigoQr' es requerido"));
            }

            Long idOperador = null;
            if (auth != null && auth.getCredentials() instanceof Long id) {
                idOperador = id;
            }

            String ip = obtenerIp(request);
            CheckInDTO resultado = checkInService.realizarCheckInPorQr(codigoQr, idOperador, ip);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<CheckInDTO>> listarTodos() {
        return ResponseEntity.ok(checkInService.obtenerTodos());
    }

    @GetMapping("/asistencia/{idAsistencia}")
    public ResponseEntity<List<CheckInDTO>> listarPorAsistencia(@PathVariable Long idAsistencia) {
        return ResponseEntity.ok(checkInService.obtenerPorAsistencia(idAsistencia));
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorDocumento(@RequestParam String numeroDocumento,
                                                @RequestParam(required = false) String tipoDocumento) {
        try {
            return ResponseEntity.ok(checkInService.buscarPorDocumento(numeroDocumento, tipoDocumento));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== CHECK-IN MASIVO ==========

    @GetMapping("/masivo/preview/{idEvento}")
    public ResponseEntity<?> previsualizarCheckInMasivo(@PathVariable Long idEvento) {
        try {
            CheckInMasivoDTO resultado = checkInService.previsualizarCheckInMasivo(idEvento);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/masivo/confirmar")
    public ResponseEntity<?> ejecutarCheckInMasivo(@RequestBody Map<String, Long> body,
                                                   Authentication auth,
                                                   HttpServletRequest request) {
        try {
            Long idEvento = body.get("idEvento");
            if (idEvento == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El campo 'idEvento' es requerido"));
            }

            Long idOperador = null;
            if (auth != null && auth.getCredentials() instanceof Long id) {
                idOperador = id;
            }

            String ip = obtenerIp(request);
            CheckInMasivoDTO resultado = checkInService.ejecutarCheckInMasivo(idEvento, idOperador, ip);
            return ResponseEntity.ok(resultado);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{idAsistencia}/anular")
    public ResponseEntity<?> anularCheckIn(@PathVariable Long idAsistencia,
                                           Authentication auth,
                                           HttpServletRequest request) {
        try {
            Long idOperador = null;
            if (auth != null && auth.getCredentials() instanceof Long id) {
                idOperador = id;
            }
            String ip = obtenerIp(request);
            CheckInDTO resultado = checkInService.anularCheckIn(idAsistencia, idOperador, ip);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
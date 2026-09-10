package Eventos.eventos.controller;

import Eventos.eventos.dto.CertificadoDTO;
import Eventos.eventos.entity.Certificado;
import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.CertificadoRepository;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.CertificadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    @Autowired
    private CertificadoService certificadoService;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    @Autowired
    private CertificadoRepository certificadoRepository;

    @PostMapping("/generar/{idRegistro}")
    public ResponseEntity<?> generarCertificado(@PathVariable Long idRegistro) {
        try {
            CertificadoDTO dto = certificadoService.generarCertificado(idRegistro);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/regenerar/{idRegistro}")
    public ResponseEntity<?> regenerarCertificado(
            @PathVariable Long idRegistro,
            @RequestParam(name = "idPlantilla", required = false) Long idPlantilla) {
        try {
            CertificadoDTO dto = certificadoService.regenerar(idRegistro, idPlantilla);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/regenerar/evento/{idEvento}")
    public ResponseEntity<?> regenerarCertificadosPorEvento(
            @PathVariable Long idEvento,
            @RequestParam(name = "idPlantilla", required = false) Long idPlantilla) {
        try {
            Map<String, Object> resultado = certificadoService.regenerarPorEvento(idEvento, idPlantilla);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{idCertificado}/pdf")
    public ResponseEntity<?> descargarPdf(
            @PathVariable Long idCertificado,
            @RequestParam(name = "idPlantilla", required = false) Long idPlantilla) {
        try {
            CertificadoDTO meta = certificadoService.obtenerPorId(idCertificado);
            byte[] pdfBytes = certificadoService.descargarPdf(idCertificado, idPlantilla);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    meta.getCodigoVerificacion() + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/generar/{idRegistro}")
    public ResponseEntity<?> generarYDescargar(
            @PathVariable Long idRegistro,
            @RequestParam(name = "idPlantilla", required = false) Long idPlantilla) {
        try {
            Certificado cert = certificadoRepository.findByRegistroAsistencia_IdRegistroAsistencia(idRegistro)
                    .orElse(null);

            CertificadoDTO dto;
            if (cert != null) {
                dto = certificadoService.obtenerPorId(cert.getIdCertificado());
            } else {
                dto = certificadoService.generarCertificado(idRegistro);
            }

            byte[] pdfBytes = certificadoService.descargarPdf(dto.getIdCertificado(), idPlantilla);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    dto.getCodigoVerificacion() + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mis-certificados")
    public ResponseEntity<?> misCertificados() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String email = auth.getPrincipal().toString();
            Usuario usuario = loginRepository.findByEmailUsuario(email)
                    .map(login -> login.getUsuario())
                    .orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            List<Participante> participantes = participanteRepository.findAllByUsuario_IdUsuario(usuario.getIdUsuario());
            List<CertificadoDTO> resultado = new ArrayList<>();

            for (Participante p : participantes) {
                List<RegistroAsistencia> registros = registroAsistenciaRepository.findByParticipante_IdParticipante(p.getIdParticipante());
                for (RegistroAsistencia r : registros) {
                    if (r.estaEliminado()) {
                        continue;
                    }
                    certificadoRepository.findByRegistroAsistencia_IdRegistroAsistencia(r.getIdRegistroAsistencia())
                            .ifPresent(cert -> {
                                CertificadoDTO dto = mapCertToDTO(cert, r);
                                resultado.add(dto);
                            });
                }
            }

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private CertificadoDTO mapCertToDTO(Certificado cert, RegistroAsistencia reg) {
        CertificadoDTO dto = new CertificadoDTO();
        dto.setIdCertificado(cert.getIdCertificado());
        dto.setIdRegistroAsistencia(reg.getIdRegistroAsistencia());
        dto.setCodigoVerificacion(cert.getCodigoVerificacionCertificado());
        dto.setEstadoCertificado(cert.getEstado() != null ? cert.getEstado().getNombreEstado() : null);
        dto.setUrlPdf(cert.getUrlPdfCertificado());
        dto.setFechaEmisionCertificado(cert.getFechaEmisionCertificado());

        if (reg.getEvento() != null) {
            dto.setNombreEvento(reg.getEvento().getNombreEvento());
            dto.setTipoEvento(reg.getEvento().getTipoEvento());
            dto.setModalidadEvento(reg.getEvento().getModalidadEvento());
            dto.setFechaInicioEvento(reg.getEvento().getFechaInicioEvento());
            dto.setFechaFinEvento(reg.getEvento().getFechaFinEvento());
            dto.setDuracionEvento(reg.getEvento().getDuracionEvento() != null
                    ? reg.getEvento().getDuracionEvento() + "h" : null);
        }

        if (reg.getParticipante() != null && reg.getParticipante().getUsuario() != null) {
            Usuario u = reg.getParticipante().getUsuario();
            StringBuilder sb = new StringBuilder();
            if (u.getPrimerNombreUsuario() != null && !u.getPrimerNombreUsuario().isBlank()) sb.append(u.getPrimerNombreUsuario().trim());
            if (u.getSegundoNombreUsuario() != null && !u.getSegundoNombreUsuario().isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(u.getSegundoNombreUsuario().trim());
            }
            if (u.getPrimerApellidoUsuario() != null && !u.getPrimerApellidoUsuario().isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(u.getPrimerApellidoUsuario().trim());
            }
            if (u.getSegundoApellidoUsuario() != null && !u.getSegundoApellidoUsuario().isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(u.getSegundoApellidoUsuario().trim());
            }
            dto.setNombreParticipante(sb.toString().trim());
            dto.setDocumentoParticipante(u.getNumeroDocumentoUsuario());
        }

        return dto;
    }

    @PatchMapping("/{idCertificado}/revocar")
    public ResponseEntity<?> revocarCertificado(@PathVariable Long idCertificado) {
        try {
            CertificadoDTO dto = certificadoService.revocarCertificado(idCertificado);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


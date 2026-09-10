package Eventos.eventos.controller;

import Eventos.eventos.dto.ConfiguracionCertificadoDTO;
import Eventos.eventos.entity.ConfiguracionCertificado;
import Eventos.eventos.repository.ConfiguracionCertificadoRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configuracion-certificado")
@CrossOrigin(origins = "*")
public class ConfiguracionCertificadoController {

    @Autowired
    private ConfiguracionCertificadoRepository repo;

    @Autowired
    private TemplateEngine templateEngine;

    /** Listar todas las plantillas guardadas */
    @GetMapping
    public ResponseEntity<List<ConfiguracionCertificadoDTO>> listarTodas() {
        List<ConfiguracionCertificado> lista = repo.findAllByActivaTrueOrderByFechaActualizacionDesc();
        if (lista.isEmpty()) {
            ConfiguracionCertificado def = defaultConfig();
            def.setEsPredeterminada(true);
            def.setNombrePlantilla("Plantilla Predeterminada");
            def.setActiva(true);
            repo.save(def);
            lista = List.of(def);
        }
        return ResponseEntity.ok(lista.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /** Obtener plantilla por ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return repo.findById(id)
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Crear nueva plantilla */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ConfiguracionCertificadoDTO dto) {
        try {
            ConfiguracionCertificado config = new ConfiguracionCertificado();
            applyDTO(config, dto);
            if (config.getNombrePlantilla() == null || config.getNombrePlantilla().isBlank()) {
                config.setNombrePlantilla("Nueva Plantilla");
            }
            if (Boolean.TRUE.equals(config.getEsPredeterminada()) || repo.count() == 0) {
                desmarcarPredeterminadas();
                config.setEsPredeterminada(true);
            }
            config.setActiva(true);
            ConfiguracionCertificado guardada = repo.save(config);
            return ResponseEntity.ok(toDTO(guardada));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Actualizar plantilla existente */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ConfiguracionCertificadoDTO dto) {
        try {
            ConfiguracionCertificado config = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con ID: " + id));

            applyDTO(config, dto);
            if (Boolean.TRUE.equals(dto.getEsPredeterminada())) {
                desmarcarPredeterminadas();
                config.setEsPredeterminada(true);
            }
            repo.save(config);
            return ResponseEntity.ok(toDTO(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Eliminar (inhabilitar) plantilla */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            ConfiguracionCertificado config = repo.findById(id)
                    .orElse(null);
            if (config == null) {
                return ResponseEntity.notFound().build();
            }
            config.setActiva(false);
            repo.save(config);
            // Asegurar que quede al menos una predeterminada activa si hay otras
            if (repo.findFirstByEsPredeterminadaTrue().isEmpty()) {
                repo.findAllByActivaTrueOrderByFechaActualizacionDesc().stream().findFirst().ifPresent(c -> {
                    c.setEsPredeterminada(true);
                    repo.save(c);
                });
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Marcar plantilla como predeterminada */
    @PatchMapping("/{id}/predeterminada")
    public ResponseEntity<?> marcarPredeterminada(@PathVariable Long id) {
        try {
            ConfiguracionCertificado config = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con ID: " + id));
            desmarcarPredeterminadas();
            config.setEsPredeterminada(true);
            repo.save(config);
            return ResponseEntity.ok(toDTO(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Previsualizar PDF con plantilla opcional idPlantilla */
    @GetMapping("/preview-pdf")
    public ResponseEntity<?> previewPdf(@RequestParam(name = "idPlantilla", required = false) Long idPlantilla) {
        try {
            ConfiguracionCertificado config = null;
            if (idPlantilla != null) {
                config = repo.findById(idPlantilla).orElse(null);
            }
            if (config == null) {
                config = repo.findFirstByEsPredeterminadaTrue()
                        .orElseGet(() -> repo.findFirstByActivaTrue().orElseGet(this::defaultConfig));
            }

            byte[] pdfBytes = generarPdfPreview(config);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "preview-certificado.pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void desmarcarPredeterminadas() {
        repo.findAll().forEach(c -> {
            if (Boolean.TRUE.equals(c.getEsPredeterminada())) {
                c.setEsPredeterminada(false);
                repo.save(c);
            }
        });
    }

    private byte[] generarPdfPreview(ConfiguracionCertificado cfg) throws Exception {
        Context ctx = new Context(Locale.of("es", "ES"));
        ctx.setVariable("organizacion",      cfg.getNombreOrganizacion() != null ? cfg.getNombreOrganizacion() : "UNIVERSIDAD EAN");
        ctx.setVariable("tituloCertificado", cfg.getTituloCertificado() != null ? cfg.getTituloCertificado() : "CERTIFICADO DE PARTICIPACIÓN");
        ctx.setVariable("textoCuerpo",       cfg.getTextoCuerpo());
        ctx.setVariable("colorBorde",        cfg.getColorBorde() != null ? cfg.getColorBorde() : "#000e2d");
        ctx.setVariable("colorAcento",       cfg.getColorAcento() != null ? cfg.getColorAcento() : "#7b580e");
        ctx.setVariable("logoBase64",        cfg.getLogoBase64());

        ctx.setVariable("nombreCompleto",    "JUAN VILLAMIL");
        ctx.setVariable("documento",         "1000521258");
        ctx.setVariable("tipoDocumento",     "Cédula de ciudadanía");
        ctx.setVariable("nombreEvento",      "Evento Recreo Deportivo");
        ctx.setVariable("duracionEvento",    "6 horas");
        ctx.setVariable("codigo",            "CERT-4E566F1B");
        ctx.setVariable("qrBase64",          null);
        ctx.setVariable("fechaFormateada",
                LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("es", "ES"))));

        // Firmas
        ctx.setVariable("firma1Nombre",       cfg.getFirma1Nombre());
        ctx.setVariable("firma1Cargo",        cfg.getFirma1Cargo());
        ctx.setVariable("firma1ImagenBase64", cfg.getFirma1ImagenBase64());
        ctx.setVariable("firma2Nombre",       cfg.getFirma2Nombre());
        ctx.setVariable("firma2Cargo",        cfg.getFirma2Cargo());
        ctx.setVariable("firma2ImagenBase64", cfg.getFirma2ImagenBase64());
        ctx.setVariable("firma3Nombre",       cfg.getFirma3Nombre());
        ctx.setVariable("firma3Cargo",        cfg.getFirma3Cargo());
        ctx.setVariable("firma3ImagenBase64", cfg.getFirma3ImagenBase64());

        String html = templateEngine.process("certificado", ctx);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        }
    }

    private void applyDTO(ConfiguracionCertificado config, ConfiguracionCertificadoDTO dto) {
        config.setNombrePlantilla(dto.getNombrePlantilla());
        config.setEsPredeterminada(dto.getEsPredeterminada());
        config.setNombreOrganizacion(dto.getNombreOrganizacion());
        config.setTituloCertificado(dto.getTituloCertificado());
        config.setTextoCuerpo(dto.getTextoCuerpo());
        config.setColorBorde(dto.getColorBorde());
        config.setColorAcento(dto.getColorAcento());
        config.setLogoBase64(dto.getLogoBase64());
        config.setFirma1Nombre(dto.getFirma1Nombre());
        config.setFirma1Cargo(dto.getFirma1Cargo());
        config.setFirma1ImagenBase64(dto.getFirma1ImagenBase64());
        config.setFirma2Nombre(dto.getFirma2Nombre());
        config.setFirma2Cargo(dto.getFirma2Cargo());
        config.setFirma2ImagenBase64(dto.getFirma2ImagenBase64());
        config.setFirma3Nombre(dto.getFirma3Nombre());
        config.setFirma3Cargo(dto.getFirma3Cargo());
        config.setFirma3ImagenBase64(dto.getFirma3ImagenBase64());
    }

    private ConfiguracionCertificadoDTO toDTO(ConfiguracionCertificado c) {
        ConfiguracionCertificadoDTO dto = new ConfiguracionCertificadoDTO();
        dto.setIdConfiguracion(c.getIdConfiguracion());
        dto.setNombrePlantilla(c.getNombrePlantilla() != null ? c.getNombrePlantilla() : "Plantilla #" + c.getIdConfiguracion());
        dto.setEsPredeterminada(Boolean.TRUE.equals(c.getEsPredeterminada()));
        dto.setNombreOrganizacion(c.getNombreOrganizacion());
        dto.setTituloCertificado(c.getTituloCertificado());
        dto.setTextoCuerpo(c.getTextoCuerpo());
        dto.setColorBorde(c.getColorBorde());
        dto.setColorAcento(c.getColorAcento());
        dto.setLogoBase64(c.getLogoBase64());
        dto.setFirma1Nombre(c.getFirma1Nombre());
        dto.setFirma1Cargo(c.getFirma1Cargo());
        dto.setFirma1ImagenBase64(c.getFirma1ImagenBase64());
        dto.setFirma2Nombre(c.getFirma2Nombre());
        dto.setFirma2Cargo(c.getFirma2Cargo());
        dto.setFirma2ImagenBase64(c.getFirma2ImagenBase64());
        dto.setFirma3Nombre(c.getFirma3Nombre());
        dto.setFirma3Cargo(c.getFirma3Cargo());
        dto.setFirma3ImagenBase64(c.getFirma3ImagenBase64());
        return dto;
    }

    private ConfiguracionCertificado defaultConfig() {
        ConfiguracionCertificado cfg = new ConfiguracionCertificado();
        cfg.setNombrePlantilla("Plantilla Predeterminada EAN");
        cfg.setEsPredeterminada(true);
        cfg.setNombreOrganizacion("UNIVERSIDAD EAN");
        cfg.setTituloCertificado("CERTIFICADO DE PARTICIPACIÓN");
        cfg.setTextoCuerpo("Por haber culminado con éxito el evento [[EVENTO]] con una duración de [[DURACION]] expedido el día [[FECHA]].");
        cfg.setColorBorde("#000e2d");
        cfg.setColorAcento("#7b580e");
        cfg.setFirma1Cargo("RECTOR");
        return cfg;
    }
}

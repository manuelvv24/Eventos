package Eventos.eventos.service.impl;

import Eventos.eventos.dto.CertificadoDTO;
import Eventos.eventos.entity.Certificado;
import Eventos.eventos.entity.Estado;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.CertificadoRepository;
import Eventos.eventos.repository.CheckInRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.CertificadoService;
import Eventos.eventos.service.CloudinaryService;
import Eventos.eventos.service.EmailService;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.NotificacionService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificadoServiceImpl implements CertificadoService {

    private final TemplateEngine templateEngine;

    @Autowired
    private CertificadoRepository certificadoRepository;

    @Autowired
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private EstadoService estadoService;

    @Autowired
    private Eventos.eventos.repository.ConfiguracionCertificadoRepository configuracionRepository;

    @Value("${certificados.organizacion:NOMBRE DE LA ORGANIZACIÓN}")
    private String nombreOrganizacion;

    @Value("${certificados.url-verificacion:http://localhost:5173/verificar/codigo?codigo=}")
    private String urlVerificacion;

    public CertificadoServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    @Transactional
    public CertificadoDTO generarCertificado(Long idRegistroAsistencia) 
    {
        RegistroAsistencia registro = registroAsistenciaRepository.findById(idRegistroAsistencia)
                .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        String estadoRegistro = registro.getEstado() != null ? registro.getEstado().getNombreEstado() : null;
        if (registro.estaEliminado()) {
            throw new RuntimeException(
                    "La inscripción de este participante está inactivada: no se puede generar el certificado");
        }
        if (estadoRegistro == null || !"ASISTIO".equalsIgnoreCase(estadoRegistro.trim())) {
            throw new RuntimeException("Solo se pueden generar certificados para participantes que asistieron");
        }

        validarDiasMinimosAsistencia(registro);
        validarParticipanteNoPersonal(registro);

        // Si ya existe un certificado para este registro, devolver el existente
        var existente = certificadoRepository.findByRegistroAsistencia_IdRegistroAsistencia(idRegistroAsistencia);
        if (existente.isPresent()) {
            return mapToDTO(existente.get());
        }

        Evento evento = registro.getEvento();
        String codigoVerificacion = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String nombreCompleto = "Participante";
        String documento = "";
        String tipoDocumento = "";

        if (registro.getParticipante() != null && registro.getParticipante().getUsuario() != null) {
            Usuario u = registro.getParticipante().getUsuario();
            nombreCompleto = construirNombreCompleto(u);
            documento = u.getNumeroDocumentoUsuario() != null ? u.getNumeroDocumentoUsuario() : "";
            tipoDocumento = u.getTipoDocumentoUsuario() != null ? u.getTipoDocumentoUsuario() : "";
        }

        String nombreEvento   = evento.getNombreEvento();
        String duracionEvento = evento.getDuracionEvento() != null
                ? evento.getDuracionEvento() + " horas" : "N/A";

     
        byte[] pdfBytes  = generarPdfEnMemoria(nombreCompleto, documento, tipoDocumento, nombreEvento, duracionEvento, codigoVerificacion, null);
        String urlPdf    = null;
        try {
            urlPdf = cloudinaryService.subirPdf(pdfBytes, codigoVerificacion, "certificados");
        } catch (Exception e) {
            System.err.println("[WARN] No se pudo subir PDF a Cloudinary (usando fallback en memoria): " + e.getMessage());
            urlPdf = "/api/certificados/generar/" + idRegistroAsistencia;
        }

        Certificado certificado = new Certificado();
        certificado.setRegistroAsistencia(registro);
        certificado.setCodigoVerificacionCertificado(codigoVerificacion);
        Estado estadoCert = estadoService.resolver(EstadoService.TIPO_CERTIFICADO, "ACTIVO");
        if (estadoCert != null) {
            certificado.setEstado(estadoCert);
        }
        certificado.setFechaEmisionCertificado(LocalDateTime.now());
        certificado.setUrlPdfCertificado(urlPdf);

        Certificado guardado = certificadoRepository.save(certificado);

        if (registro.getParticipante() != null && registro.getParticipante().getUsuario() != null) {
            Usuario usuario = registro.getParticipante().getUsuario();

            notificacionService.enviar(
                    usuario,
                    evento,
                    "CERTIFICADO",
                    "Certificado disponible",
                    "Tu certificado del evento \"" + nombreEvento + "\" ya está listo. Código: " + codigoVerificacion
            );

            if (usuario.getLogin() != null && usuario.getLogin().getEmailUsuario() != null
                    && !usuario.getLogin().getEmailUsuario().isEmpty()) {
                String urlPublica = urlVerificacion + codigoVerificacion;
                emailService.enviarSeguro(
                        usuario.getLogin().getEmailUsuario(),
                        "🎓 Tu certificado está listo: " + nombreEvento,
                        plantillaCertificado(usuario, evento, codigoVerificacion, urlPublica, urlPdf)
                );
            }
        }

        return mapToDTO(guardado);
    }
    // ── Validación de días mínimos de asistencia ─────────────────────────────

    /**
     * Si el evento configura diasMinimosCertificacion > 0, exige que el participante
     * tenga check-ins registrados en al menos ese número de días distintos
     * (contando los días reales del evento, fecha inicio a fecha fin).
     */
    private void validarDiasMinimosAsistencia(RegistroAsistencia registro) {
        Evento evento = registro.getEvento();
        if (evento == null) return;

        Integer minimoConfigurado = evento.getDiasMinimosCertificacion();
        if (minimoConfigurado == null || minimoConfigurado <= 0) return;

        long diasEvento = calcularDiasEvento(evento);
        long requeridos = Math.min(minimoConfigurado, diasEvento);

        long diasAsistidos = checkInRepository
                .findByRegistroAsistencia_IdRegistroAsistencia(registro.getIdRegistroAsistencia())
                .stream()
                .map(c -> c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        if (diasAsistidos < requeridos) {
            long faltan = requeridos - diasAsistidos;
            throw new RuntimeException(String.format(
                    "Este evento exige asistir a %d de %d día(s) para certificar. "
                    + "El participante solo tiene check-in en %d día(s): le falta(n) %d día(s) más para cumplir el requisito.",
                    requeridos, diasEvento, diasAsistidos, faltan));
        }
    }

    private long calcularDiasEvento(Evento evento) {
        if (evento.getFechaInicioEvento() == null || evento.getFechaFinEvento() == null) {
            return 1;
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(
                evento.getFechaInicioEvento().toLocalDate(),
                evento.getFechaFinEvento().toLocalDate()) + 1;
        return Math.max(dias, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificadoDTO verificarPorCodigo(String codigoVerificacion) {
        Certificado certificado = certificadoRepository.findByCodigoVerificacionCertificado(codigoVerificacion)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));
        return mapToDTO(certificado);
    }

    // ── Descargar PDF ─────────────────────────────────────────────────────────

    // ── Descargar PDF ─────────────────────────────────────────────────────────

    @Override
    public byte[] descargarPdf(Long idCertificado) {
        return descargarPdf(idCertificado, null);
    }

    @Override
    public byte[] descargarPdf(Long idCertificado, Long idPlantilla) {
        Certificado certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));

        // Si no se especificó plantilla y existe URL de Cloudinary, se usa Cloudinary
        String urlPdf = certificado.getUrlPdfCertificado();
        if (idPlantilla == null && urlPdf != null && urlPdf.startsWith("http")) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlPdf))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (Exception e) {
                System.err.println("[WARN] Error al descargar de Cloudinary, regenerando en memoria: " + e.getMessage());
            }
        }

        // Regenerar PDF en memoria usando la plantilla elegida o predeterminada
        RegistroAsistencia registro = certificado.getRegistroAsistencia();
        Evento evento = registro.getEvento();
        String nombreCompleto = "Participante";
        String documento = "";
        String tipoDocumento = "";

        if (registro.getParticipante() != null && registro.getParticipante().getUsuario() != null) {
            Usuario u = registro.getParticipante().getUsuario();
            nombreCompleto = construirNombreCompleto(u);
            documento = u.getNumeroDocumentoUsuario() != null ? u.getNumeroDocumentoUsuario() : "";
            tipoDocumento = u.getTipoDocumentoUsuario() != null ? u.getTipoDocumentoUsuario() : "";
        }

        String nombreEvento   = evento.getNombreEvento();
        String duracionEvento = evento.getDuracionEvento() != null
                ? evento.getDuracionEvento() + " horas" : "N/A";

        return generarPdfEnMemoria(nombreCompleto, documento, tipoDocumento, nombreEvento, duracionEvento, certificado.getCodigoVerificacionCertificado(), idPlantilla);
    }

    // ── Obtener / Revocar ─────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public CertificadoDTO obtenerPorId(Long idCertificado) {
         Certificado certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));
        return mapToDTO(certificado);
    }

    @Override
    public CertificadoDTO revocarCertificado(Long idCertificado) {
        Certificado certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));
        Estado estadoRevocado = estadoService.resolver(EstadoService.TIPO_CERTIFICADO, "REVOCADO");
        if (estadoRevocado != null) {
            certificado.setEstado(estadoRevocado);
        }
        return mapToDTO(certificadoRepository.save(certificado));
    }

    // ── Regenerar PDF con otra plantilla ─────────────────────────────────────

    @Override
    @Transactional
    public CertificadoDTO regenerar(Long idRegistroAsistencia, Long idPlantilla) {
        Certificado certificado = certificadoRepository
                .findByRegistroAsistencia_IdRegistroAsistencia(idRegistroAsistencia)
                .orElseThrow(() -> new RuntimeException("El certificado no existe para este registro de asistencia"));

        if (esRevocado(certificado)) {
            throw new RuntimeException("No se puede regenerar un certificado revocado");
        }

        validarParticipanteNoPersonal(certificado.getRegistroAsistencia());

        RegistroAsistencia registro = certificado.getRegistroAsistencia();
        Evento evento = registro.getEvento();
        String nombreCompleto = "Participante";
        String documento = "";
        String tipoDocumento = "";

        if (registro.getParticipante() != null && registro.getParticipante().getUsuario() != null) {
            Usuario u = registro.getParticipante().getUsuario();
            nombreCompleto = construirNombreCompleto(u);
            documento = u.getNumeroDocumentoUsuario() != null ? u.getNumeroDocumentoUsuario() : "";
            tipoDocumento = u.getTipoDocumentoUsuario() != null ? u.getTipoDocumentoUsuario() : "";
        }

        String nombreEvento   = evento.getNombreEvento();
        String duracionEvento = evento.getDuracionEvento() != null
                ? evento.getDuracionEvento() + " horas" : "N/A";

        // Mismo public_id (código de verificación) → Cloudinary sobrescribe conservando la URL
        byte[] pdfBytes = generarPdfEnMemoria(nombreCompleto, documento, tipoDocumento, nombreEvento, duracionEvento,
                certificado.getCodigoVerificacionCertificado(), idPlantilla);

        try {
            String urlPdf = cloudinaryService.subirPdf(
                    pdfBytes, certificado.getCodigoVerificacionCertificado(), "certificados");
            certificado.setUrlPdfCertificado(urlPdf);
        } catch (Exception e) {
            System.err.println("[WARN] No se pudo sobrescribir el PDF en Cloudinary al regenerar "
                    + certificado.getCodigoVerificacionCertificado() + ": " + e.getMessage());
        }

        return mapToDTO(certificadoRepository.save(certificado));
    }

    @Override
    @Transactional
    public Map<String, Object> regenerarPorEvento(Long idEvento, Long idPlantilla) {
        List<RegistroAsistencia> registros = registroAsistenciaRepository.findByEvento_IdEventos(idEvento);
        int total = 0;
        int regenerados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();

        for (RegistroAsistencia r : registros) {
            if (r.estaEliminado()) {
                continue;
            }
            total++;
            Optional<Certificado> opt = certificadoRepository
                    .findByRegistroAsistencia_IdRegistroAsistencia(r.getIdRegistroAsistencia());
            if (opt.isEmpty()) {
                continue;
            }
            Certificado cert = opt.get();
            if (esRevocado(cert)) {
                omitidos++;
                continue;
            }
            try {
                regenerar(r.getIdRegistroAsistencia(), idPlantilla);
                regenerados++;
            } catch (RuntimeException e) {
                omitidos++;
                errores.add(cert.getCodigoVerificacionCertificado() + ": " + e.getMessage());
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("total", total);
        resultado.put("regenerados", regenerados);
        resultado.put("omitidos", omitidos);
        resultado.put("errores", errores);
        return resultado;
    }

    private boolean esRevocado(Certificado certificado) {
        return certificado.getEstado() != null
                && "REVOCADO".equalsIgnoreCase(certificado.getEstado().getNombreEstado());
    }

    /**
     * El personal organizador (Admin, Super Admin, Operador, Monitor) no forma parte
     * de la audiencia de los eventos: no se le emiten certificados aunque aparezca
     * registrado (p. ej. inscripciones previas a esta restricción).
     */
    private void validarParticipanteNoPersonal(RegistroAsistencia registro) {
        if (registro == null || registro.getParticipante() == null
                || registro.getParticipante().getUsuario() == null) {
            return;
        }
        Usuario usuario = registro.getParticipante().getUsuario();
        if (Eventos.eventos.util.RolNames.esPersonal(usuario.getRoles())) {
            throw new RuntimeException(
                    "No se puede generar un certificado para personal organizador del sistema.");
        }
    }

    // ── Generación PDF en memoria ─────────────────────────────────────────────

    private byte[] generarPdfEnMemoria(String nombreCompleto, String documento, String tipoDocumento,
                                       String nombreEvento, String duracionEvento,
                                       String codigo, Long idPlantilla) {
        try {
            String qrBase64 = generarQrBase64(urlVerificacion + codigo, 150, 150);

            Eventos.eventos.entity.ConfiguracionCertificado cfg = null;
            if (idPlantilla != null) {
                cfg = configuracionRepository.findById(idPlantilla).orElse(null);
            }
            if (cfg == null) {
                cfg = configuracionRepository.findFirstByEsPredeterminadaTrue()
                        .orElseGet(() -> configuracionRepository.findFirstByActivaTrue().orElse(null));
            }

            Context context = new Context(Locale.of("es", "ES"));

            String orgName = (cfg != null && cfg.getNombreOrganizacion() != null && !cfg.getNombreOrganizacion().isEmpty())
                    ? cfg.getNombreOrganizacion() : nombreOrganizacion;
            String titulo = (cfg != null && cfg.getTituloCertificado() != null && !cfg.getTituloCertificado().isEmpty())
                    ? cfg.getTituloCertificado() : "Certificado de Participación";
            String colorBorde = (cfg != null && cfg.getColorBorde() != null) ? cfg.getColorBorde() : "#000e2d";
            String colorAcento = (cfg != null && cfg.getColorAcento() != null) ? cfg.getColorAcento() : "#7b580e";
            String textoCuerpo = prepararTextoCuerpo(cfg, nombreEvento, duracionEvento, colorBorde);

            context.setVariable("organizacion",   orgName);
            context.setVariable("tituloCertificado", titulo);
            context.setVariable("colorBorde",      colorBorde);
            context.setVariable("colorAcento",     colorAcento);
            context.setVariable("textoCuerpo",     textoCuerpo);
            context.setVariable("logoBase64",      cfg != null ? cfg.getLogoBase64() : null);

            context.setVariable("firma1Nombre",       cfg != null ? cfg.getFirma1Nombre() : null);
            context.setVariable("firma1Cargo",        cfg != null ? cfg.getFirma1Cargo() : "DIRECCIÓN GENERAL");
            context.setVariable("firma1ImagenBase64", cfg != null ? cfg.getFirma1ImagenBase64() : null);

            context.setVariable("firma2Nombre",       cfg != null ? cfg.getFirma2Nombre() : null);
            context.setVariable("firma2Cargo",        cfg != null ? cfg.getFirma2Cargo() : null);
            context.setVariable("firma2ImagenBase64", cfg != null ? cfg.getFirma2ImagenBase64() : null);

            context.setVariable("firma3Nombre",       cfg != null ? cfg.getFirma3Nombre() : null);
            context.setVariable("firma3Cargo",        cfg != null ? cfg.getFirma3Cargo() : null);
            context.setVariable("firma3ImagenBase64", cfg != null ? cfg.getFirma3ImagenBase64() : null);

            context.setVariable("nombreCompleto", nombreCompleto.toUpperCase(Locale.ROOT));
            context.setVariable("full_name",      nombreCompleto.toUpperCase(Locale.ROOT));
            context.setVariable("documento",      documento);
            context.setVariable("tipoDocumento",  tipoDocumentoLegible(tipoDocumento));
            context.setVariable("nombreEvento",   nombreEvento);
            context.setVariable("duracionEvento", duracionEvento);
            context.setVariable("codigo",         codigo);
            context.setVariable("verification_id", codigo);
            context.setVariable("qrBase64",       qrBase64);

            String fechaFormateada = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("es", "ES")));
            context.setVariable("fechaFormateada", fechaFormateada);

            String htmlContent = templateEngine.process("certificado", context);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(htmlContent, null);
                builder.toStream(baos);
                builder.run();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF del certificado: " + e.getMessage(), e);
        }
    }

    // ── QR ───────────────────────────────────────────────────────────────────

    private String generarQrBase64(String contenido, int ancho, int alto)
            throws WriterException, IOException {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix   = qrWriter.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    /**
     * Texto del cuerpo del certificado. Usa el texto configurado en la plantilla
     * (con placeholders [[EVENTO]] y [[DURACION]]) o, si no hay plantilla configurada,
     * el texto por defecto con el color del borde.
     */
    private String prepararTextoCuerpo(Eventos.eventos.entity.ConfiguracionCertificado cfg, String nombreEvento,
                                       String duracionEvento, String colorBorde) {
        String cuerpo = (cfg != null && cfg.getTextoCuerpo() != null && !cfg.getTextoCuerpo().isBlank())
                ? cfg.getTextoCuerpo()
                : "Por su destacada participación en <span class=\"body-highlight\" style=\"color: "
                + colorBorde + "\">[[EVENTO]]</span>,"
                + " con una duración de <span class=\"body-highlight\" style=\"color: "
                + colorBorde + "\">[[DURACION]]</span>,"
                + " demostrando compromiso y excelencia académica.";
        cuerpo = cuerpo.replace("[[EVENTO]]", nombreEvento != null ? nombreEvento : "");
        cuerpo = cuerpo.replace("[[DURACION]]", duracionEvento != null ? duracionEvento : "");
        return cuerpo;
    }

    private String tipoDocumentoLegible(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        switch (codigo.trim().toUpperCase()) {
            case "CC": return "Cédula de ciudadanía";
            case "TI": return "Tarjeta de identidad";
            case "CE": return "Cédula de extranjería";
            case "PA": return "Pasaporte";
            case "NIT": return "NIT";
            case "RC": return "Registro civil";
            default:  return codigo;
        }
    }

    private String construirNombreCompleto(Usuario u) {
        if (u == null) return "Participante";
        StringBuilder sb = new StringBuilder();
        if (u.getPrimerNombreUsuario() != null && !u.getPrimerNombreUsuario().isBlank()) {
            sb.append(u.getPrimerNombreUsuario().trim());
        }
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
        String res = sb.toString().trim();
        if (res.isEmpty()) {
            String email = u.getLogin() != null ? u.getLogin().getEmailUsuario() : null;
            return email != null ? email : "Participante";
        }
        return res;
    }

    // ── Plantillas email ──────────────────────────────────────────────────────

    private String plantillaCertificado(Usuario usuario, Evento evento, String codigo,
                                        String urlVerificacionPublica, String urlDescargaPdf) {
        String nombre = construirNombreCompleto(usuario);

        return "<div style='font-family:Arial;max-width:600px;margin:auto;border:1px solid #ddd'>"
                + "<div style='background:#16a34a;color:#fff;padding:16px'><h2>¡Felicitaciones!</h2></div>"
                + "<div style='padding:20px'>"
                + "<p>Hola <b>" + nombre + "</b>,</p>"
                + "<p>Tu certificado del evento <b>" + evento.getNombreEvento() + "</b> ya está disponible.</p>"
                + "<p><b>Código de verificación:</b> " + codigo + "</p>"
                + "<p style='text-align:center;margin:20px 0'>"
                +   "<a href='" + urlVerificacionPublica + "' style='background:#2563eb;color:#fff;padding:12px 24px;"
                +   "text-decoration:none;border-radius:6px;display:inline-block;margin-right:8px'>Verificar certificado</a>"
                +   "<a href='" + urlDescargaPdf + "' style='background:#16a34a;color:#fff;padding:12px 24px;"
                +   "text-decoration:none;border-radius:6px;display:inline-block'>Descargar PDF</a>"
                + "</p>"
                + "<p style='font-size:12px;color:#666'>Verificar en: <br>" + urlVerificacionPublica + "</p>"
                + "</div></div>";
    }

    // ── Mapeo DTO ─────────────────────────────────────────────────────────────

    private CertificadoDTO mapToDTO(Certificado certificado) {
        CertificadoDTO dto = new CertificadoDTO();
        dto.setIdCertificado(certificado.getIdCertificado());
        dto.setIdRegistroAsistencia(certificado.getRegistroAsistencia().getIdRegistroAsistencia());
        dto.setCodigoVerificacion(certificado.getCodigoVerificacionCertificado());
        dto.setEstadoCertificado(certificado.getEstado() != null
                ? certificado.getEstado().getNombreEstado() : null);
        dto.setUrlPdf(certificado.getUrlPdfCertificado());
        dto.setFechaEmisionCertificado(certificado.getFechaEmisionCertificado());

        RegistroAsistencia registro = certificado.getRegistroAsistencia();
        if (registro != null) {
            if (registro.getParticipante() != null && registro.getParticipante().getUsuario() != null) {
                Usuario u = registro.getParticipante().getUsuario();
                dto.setNombreParticipante(construirNombreCompleto(u));
                dto.setDocumentoParticipante(u.getNumeroDocumentoUsuario());
            }

            Evento evento = registro.getEvento();
            if (evento != null) {
                dto.setNombreEvento(evento.getNombreEvento());
                dto.setDuracionEvento(evento.getDuracionEvento() != null
                        ? evento.getDuracionEvento() + " horas" : null);
                dto.setTipoEvento(evento.getTipoEvento());
                dto.setModalidadEvento(evento.getModalidadEvento());
                dto.setFechaInicioEvento(evento.getFechaInicioEvento());
                dto.setFechaFinEvento(evento.getFechaFinEvento());
            }
        }
        return dto;
    }
}

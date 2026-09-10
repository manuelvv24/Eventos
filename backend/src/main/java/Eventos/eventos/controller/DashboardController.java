package Eventos.eventos.controller;

import Eventos.eventos.dto.EventoDTO;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.repository.CertificadoRepository;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private EventoRepository eventoRepository;
    @Autowired private RegistroAsistenciaRepository asistenciaRepository;
    @Autowired private ParticipanteRepository participanteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CertificadoRepository certificadoRepository;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumenGeneral() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEventos",         eventoRepository.count());
        stats.put("totalParticipantes",   participanteRepository.count());
        stats.put("totalUsuarios",        usuarioRepository.count());
        stats.put("totalCertificados",    certificadoRepository.count());
        stats.put("totalRegistros",       asistenciaRepository.count());
        long eventosActivos = eventoRepository.findByEstado_NombreEstadoIgnoreCase("ACTIVO").size();
        stats.put("eventosActivos", eventosActivos);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/eventos")
    public ResponseEntity<List<Map<String, Object>>> eventosConAforo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        List<Evento> eventos = eventoRepository.findAll().stream()
                .filter(e -> tipo      == null || tipo.equalsIgnoreCase(e.getTipoEvento()))
                .filter(e -> modalidad == null || modalidad.equalsIgnoreCase(e.getModalidadEvento()))
                .filter(e -> estado    == null || (e.getEstado() != null
                        && estado.equalsIgnoreCase(e.getEstado().getNombreEstado())))
                .filter(e -> desde     == null || e.getFechaInicioEvento() == null || !e.getFechaInicioEvento().isBefore(desde))
                .filter(e -> hasta     == null || e.getFechaInicioEvento() == null || !e.getFechaInicioEvento().isAfter(hasta))
                .collect(Collectors.toList());
        List<Map<String, Object>> resultado = eventos.stream().map(e -> {
            Map<String, Object> item = new HashMap<>();
            item.put("idEventos",          e.getIdEventos());
            item.put("nombreEvento",        e.getNombreEvento());
            item.put("tipoEvento",          e.getTipoEvento());
            item.put("modalidadEvento",     e.getModalidadEvento());
            item.put("estadoEvento",        e.getEstado() != null ? e.getEstado().getNombreEstado() : null);
            item.put("fechaInicioEvento",   e.getFechaInicioEvento());
            item.put("fechaFinEvento",      e.getFechaFinEvento());
            item.put("aforoMaximo",         e.getAforoMaximoEvento());
            item.put("duracionEvento",      e.getDuracionEvento());

            long registrados  = eventoRepository.contarRegistradosPorEvento(e.getIdEventos());
            long asistieron   = asistenciaRepository.contarAsistenciasPorEvento(e.getIdEventos());
            int aforo         = e.getAforoMaximoEvento() != null ? e.getAforoMaximoEvento() : 0;
            double pctAforo   = aforo > 0 ? Math.round((registrados * 100.0 / aforo) * 10) / 10.0 : 0;
            double pctAsist   = registrados > 0 ? Math.round((asistieron * 100.0 / registrados) * 10) / 10.0 : 0;

            item.put("registrados",          registrados);
            item.put("asistieron",           asistieron);
            item.put("aforoDisponible",      Math.max(0, aforo - registrados));
            item.put("porcentajeAforo",      pctAforo);
            item.put("porcentajeAsistencia", pctAsist);
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/eventos/{idEvento}/asistencia")
    public ResponseEntity<?> asistenciaPorEvento(@PathVariable Long idEvento) {
        Evento evento = eventoRepository.findById(idEvento)
                .filter(e -> !Boolean.TRUE.equals(e.getEliminado()))
                .orElse(null);
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }

        List<Eventos.eventos.entity.RegistroAsistencia> regs = asistenciaRepository
                .findByEvento_IdEventos(idEvento).stream()
                .filter(r -> !r.estaEliminado())
                .toList();
        long registrados = regs.size();
        long asistieron  = regs.stream()
                .filter(r -> r.getEstado() != null
                        && "ASISTIO".equalsIgnoreCase(r.getEstado().getNombreEstado()))
                .count();
        int aforo        = evento.getAforoMaximoEvento() != null ? evento.getAforoMaximoEvento() : 0;

        Map<String, Object> detalle = new HashMap<>();
        detalle.put("idEvento",              idEvento);
        detalle.put("nombreEvento",           evento.getNombreEvento());
        detalle.put("aforoMaximo",            aforo);
        detalle.put("registrados",            registrados);
        detalle.put("asistieron",             asistieron);
        detalle.put("ausentes",               registrados - asistieron);
        detalle.put("aforoDisponible",        Math.max(0, aforo - registrados));
        detalle.put("porcentajeOcupacion",    aforo > 0
                ? Math.round((registrados * 100.0 / aforo) * 10) / 10.0 : 0);
        detalle.put("porcentajeAsistencia",   registrados > 0
                ? Math.round((asistieron * 100.0 / registrados) * 10) / 10.0 : 0);

        return ResponseEntity.ok(detalle);
    }

    @GetMapping("/participantes/{idParticipante}/historial")
    public ResponseEntity<?> historialParticipante(@PathVariable Long idParticipante) {
        var registros = asistenciaRepository.findByIdParticipantesWithEvento(idParticipante).stream()
                .filter(r -> !r.estaEliminado())
                .toList();
        List<Map<String, Object>> historial = registros.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("idRegistro",       r.getIdRegistroAsistencia());
            item.put("estadoAsistencia", r.getEstado() != null ? r.getEstado().getNombreEstado() : null);
            item.put("fechaAsistencia",  r.getFechaAsistencia());
            item.put("codigoQr",         r.getCodigoQrInscripcion());
            if (r.getEvento() != null) {
                item.put("idEvento",         r.getEvento().getIdEventos());
                item.put("nombreEvento",     r.getEvento().getNombreEvento());
                item.put("tipoEvento",       r.getEvento().getTipoEvento());
                item.put("modalidadEvento",  r.getEvento().getModalidadEvento());
                item.put("fechaInicio",      r.getEvento().getFechaInicioEvento());
            }
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<Map<String, Object>>> heatmapAsistencia() {
        List<Map<String, Object>> data = asistenciaRepository.heatmapAsistencia().stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("dia",   p.getDia());
            item.put("hora",  p.getHora());
            item.put("total", p.getTotal());
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/graficos")
    public ResponseEntity<Map<String, Object>> datosGraficos() {
        List<Evento> eventos = eventoRepository.findAll();

        List<String> nombresEventos = eventos.stream()
                .map(Evento::getNombreEvento)
                .collect(Collectors.toList());

        List<Long> asistenciaPorEvento = eventos.stream()
                .map(e -> asistenciaRepository.contarAsistenciasPorEvento(e.getIdEventos()))
                .collect(Collectors.toList());

        Map<String, Long> porModalidad = agrupar(eventos, Evento::getModalidadEvento);

        Map<String, Long> porTipo = agrupar(eventos, Evento::getTipoEvento);

        List<String> meses = new ArrayList<>();
        List<Long> registrosPorMes = new ArrayList<>();
        asistenciaRepository.registrosPorMes().forEach(m -> {
            meses.add(m.getMes());
            registrosPorMes.add(m.getTotal());
        });

        List<LocalDateTime> fechasInicioEvento = eventos.stream()
                .map(Evento::getFechaInicioEvento)
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("nombresEventos",      nombresEventos);
        resultado.put("asistenciaPorEvento", asistenciaPorEvento);
        resultado.put("fechasInicioEvento",  fechasInicioEvento);
        resultado.put("eventosPorModalidad", porModalidad);
        resultado.put("eventosPorTipo",      porTipo);
        resultado.put("meses",               meses);
        resultado.put("registrosPorMes",     registrosPorMes);

        return ResponseEntity.ok(resultado);
    }

    /** Normaliza una etiqueta (sin tildes y en mayusculas) para evitar graficas duplicadas por acentos/case. */
    private static String normalizar(String valor) {
        if (valor == null) {
            return "SIN DEFINIR";
        }
        return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
    }

    /** Agrúpa por clave normalizada pero muestra la etiqueta canónica capitalizada (con acentos originales). */
    private static Map<String, Long> agrupar(List<Evento> eventos, Function<Evento, String> extractor) {
        Map<String, Long> conteo = new HashMap<>();
        Map<String, String> etiqueta = new LinkedHashMap<>();
        for (Evento e : eventos) {
            String original = extractor.apply(e);
            String clave = normalizar(original);
            conteo.merge(clave, 1L, Long::sum);
            etiqueta.putIfAbsent(clave, capitalizar(original));
        }
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, String> en : etiqueta.entrySet()) {
            resultado.put(en.getValue(), conteo.get(en.getKey()));
        }
        return resultado;
    }

    /** Primera letra de cada palabra en mayúscula y el resto en minúsculas, conservando acentos. */
    private static String capitalizar(String valor) {
        if (valor == null) {
            return "Sin definir";
        }
        String[] palabras = valor.toLowerCase().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
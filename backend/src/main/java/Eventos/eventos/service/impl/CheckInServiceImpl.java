package Eventos.eventos.service.impl;

import Eventos.eventos.dto.CheckInDTO;
import Eventos.eventos.dto.CheckInMasivoDTO;
import Eventos.eventos.dto.RegistroBusquedaDTO;
import Eventos.eventos.entity.CheckIn;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.repository.CheckInRepository;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.CheckInService;
import Eventos.eventos.service.EstadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckInServiceImpl implements CheckInService {

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private RegistroAsistenciaRepository asistenciaRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private EstadoService estadoService;

    @Value("${checkin.margen-antes-min:60}")
    private int margenAntesMin;

    @Value("${checkin.porcentaje-cierre:50}")
    private int porcentajeCierre;

    private String nombreEstado(RegistroAsistencia r) {
        return r.getEstado() != null ? r.getEstado().getNombreEstado() : null;
    }

    private boolean esAsistio(RegistroAsistencia r) {
        String estado = nombreEstado(r);
        return estado != null && "ASISTIO".equalsIgnoreCase(estado.trim());
    }

    /** true si ya existe un check-in de este registro con fecha de hoy. */
    private boolean yaRegistroHoy(RegistroAsistencia r) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        return checkInRepository.findByRegistroAsistencia_IdRegistroAsistencia(r.getIdRegistroAsistencia())
                .stream()
                .anyMatch(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().equals(hoy));
    }

    private String nombreEstadoEvento(Evento e) {
        return e.getEstado() != null ? e.getEstado().getNombreEstado() : null;
    }

    @Override
    @Transactional
    public CheckInDTO realizarCheckIn(CheckInDTO dto) {
        RegistroAsistencia asistencia = asistenciaRepository.findById(dto.getIdRegistroAsistencia())
                .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        return procesarCheckIn(asistencia, dto.getMetodoCheckIn(), dto.getIdUsuario(), dto.getIpCheckIn());
    }

    @Transactional
    public CheckInDTO realizarCheckInPorQr(String codigoQr, Long idOperador, String ip) {
        RegistroAsistencia asistencia = asistenciaRepository
                .findByCodigoQrInscripcion(codigoQr)
                .orElseThrow(() -> new RuntimeException(
                        "Código QR inválido o no registrado: " + codigoQr));

        if (esAsistio(asistencia) && yaRegistroHoy(asistencia)) {
            throw new RuntimeException(
                    "Check-in ya fue realizado hoy para este participante en este evento.");
        }

        return procesarCheckIn(asistencia, "QR", idOperador, ip);
    }

    private CheckInDTO procesarCheckIn(RegistroAsistencia asistencia,
                                       String metodo, Long idOperador, String ip) {

        if (asistencia.estaEliminado()) {
            throw new RuntimeException(
                    "La inscripción de este participante está inactivada: no se permite el check-in.");
        }

        if (esAsistio(asistencia) && yaRegistroHoy(asistencia)) {
            throw new RuntimeException(
                    "Check-in ya fue realizado hoy para este participante en este evento.");
        }

        Evento evento = asistencia.getEvento();
        if (evento != null) {
            String estado = nombreEstadoEvento(evento);
            if (estado != null && (estado.equalsIgnoreCase("CANCELADO")
                    || estado.equalsIgnoreCase("FINALIZADO")
                    || estado.equalsIgnoreCase("BORRADOR"))) {
                throw new RuntimeException("No se permite el check-in: el evento está " + estado);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // HU06: ventana temporal — apertura = inicio - margen (60 min por defecto)
            LocalDateTime ahora = LocalDateTime.now();
            if (evento.getFechaInicioEvento() != null) {
                LocalDateTime apertura = evento.getFechaInicioEvento().minusMinutes(margenAntesMin);
                if (ahora.isBefore(apertura)) {
                    throw new RuntimeException("No se permite el check-in antes de la hora habilitada. "
                            + "El evento inicia el " + evento.getFechaInicioEvento().format(fmt)
                            + ". El check-in se habilita a partir de las "
                            + apertura.format(fmt) + " (" + margenAntesMin + " min antes).");
                }

                // Cierre = inicio + 50% de la duración
                LocalDateTime fechaFin = evento.getFechaFinEvento();
                if (fechaFin == null) {
                    throw new RuntimeException("No se permite el check-in: el evento no tiene fecha de fin "
                            + "definida; no es posible habilitar el check-in.");
                }
                long duracionMin = Duration.between(evento.getFechaInicioEvento(), fechaFin).toMinutes();
                LocalDateTime cierre = evento.getFechaInicioEvento()
                        .plusMinutes(duracionMin * porcentajeCierre / 100);
                if (ahora.isAfter(cierre)) {
                    throw new RuntimeException("No se permite el check-in: ya transcurrió el "
                            + porcentajeCierre + "% del evento. Solo se permite hasta las "
                            + cierre.format(fmt) + " (el evento finaliza a las "
                            + fechaFin.format(fmt) + ").");
                }
            }
        }

        asistencia.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "ASISTIO"));
        asistencia.setFechaCheckIn(LocalDateTime.now());
        asistencia.setMetodoCheckIn(metodo != null ? metodo : "MANUAL");
        asistencia.setIpCheckIn(ip);
        asistenciaRepository.save(asistencia);

        CheckIn checkIn = new CheckIn();
        checkIn.setRegistroAsistencia(asistencia);
        checkIn.setMetodoCheckIn(metodo != null ? metodo : "MANUAL");
        checkIn.setIdUsuario(idOperador);
        checkIn.setIpCheckIn(ip);

        CheckIn guardado = checkInRepository.save(checkIn);
        return mapToDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInDTO> obtenerTodos() {

        return checkInRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInDTO> obtenerPorAsistencia(Long idRegistroAsistencia) {
        return checkInRepository.findByRegistroAsistencia_IdRegistroAsistencia(idRegistroAsistencia)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroBusquedaDTO> buscarPorDocumento(String numeroDocumento, String tipoDocumento) {
        String doc = numeroDocumento == null ? "" : numeroDocumento.trim();
        if (doc.isEmpty()) {
            throw new RuntimeException("El número de identificación es obligatorio");
        }

        return asistenciaRepository.findByNumeroDocumento(doc).stream()
                .filter(r -> !r.estaEliminado())
                .filter(r -> tipoDocumento == null || tipoDocumento.isBlank()
                        || (r.getParticipante() != null
                            && r.getParticipante().getUsuario() != null
                            && tipoDocumento.equalsIgnoreCase(r.getParticipante().getUsuario().getTipoDocumentoUsuario())))
                .map(this::mapToBusquedaDTO)
                .collect(Collectors.toList());
    }

    private RegistroBusquedaDTO mapToBusquedaDTO(RegistroAsistencia r) {
        RegistroBusquedaDTO dto = new RegistroBusquedaDTO();
        dto.setIdRegistroAsistencia(r.getIdRegistroAsistencia());
        dto.setYaAsistio(esAsistio(r));
        dto.setFechaCheckIn(r.getFechaCheckIn());
        dto.setEstadoRegistro(nombreEstado(r));
        if (r.getEvento() != null) {
            dto.setIdEventos(r.getEvento().getIdEventos());
            dto.setNombreEvento(r.getEvento().getNombreEvento());
            dto.setFechaInicioEvento(r.getEvento().getFechaInicioEvento());
        }
        if (r.getParticipante() != null && r.getParticipante().getUsuario() != null) {
            var u = r.getParticipante().getUsuario();
            dto.setTipoDocumento(u.getTipoDocumentoUsuario());
            dto.setNumeroDocumento(u.getNumeroDocumentoUsuario());
            dto.setNombreCompleto(String.join(" ",
                    nz(u.getPrimerNombreUsuario()), nz(u.getPrimerApellidoUsuario())).trim());
        }
        return dto;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CheckInDTO mapToDTO(CheckIn checkIn) {
        CheckInDTO dto = new CheckInDTO();
        dto.setIdCheckIn(checkIn.getIdCheckIn());
        dto.setIdRegistroAsistencia(checkIn.getRegistroAsistencia().getIdRegistroAsistencia());
        dto.setMetodoCheckIn(checkIn.getMetodoCheckIn());
        dto.setIdUsuario(checkIn.getIdUsuario());
        dto.setIpCheckIn(checkIn.getIpCheckIn());
        dto.setCreatedAt(checkIn.getCreatedAt());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckInMasivoDTO previsualizarCheckInMasivo(Long idEvento) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("El evento está en la papelera y no admite check-in.");
        }

        validarEstadoEvento(evento);

        List<RegistroAsistencia> registros = asistenciaRepository.findByEvento_IdEventos(idEvento).stream()
                .filter(r -> !r.estaEliminado())
                .collect(Collectors.toList());
        
        CheckInMasivoDTO dto = new CheckInMasivoDTO();
        dto.setIdEvento(idEvento);
        dto.setNombreEvento(evento.getNombreEvento());
        dto.setTotalInscritos(registros.size());
        
        long pendientes = registros.stream()
                .filter(r -> !esAsistio(r))
                .count();
        
        long yaAsistieron = registros.stream()
                .filter(r -> esAsistio(r))
                .count();
        
        dto.setPendientes((int) pendientes);
        dto.setYaAsistieron((int) yaAsistieron);
        dto.setMensaje(String.format("%d pendientes de check-in, %d ya asistieron", pendientes, yaAsistieron));
        
        return dto;
    }

    @Override
    @Transactional
    public CheckInMasivoDTO ejecutarCheckInMasivo(Long idEvento, Long idOperador, String ip) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("El evento está en la papelera y no admite check-in.");
        }

        validarEstadoEvento(evento);

        List<RegistroAsistencia> registros = asistenciaRepository.findByEvento_IdEventos(idEvento).stream()
                .filter(r -> !r.estaEliminado())
                .collect(Collectors.toList());
        
        List<RegistroAsistencia> pendientes = registros.stream()
                .filter(r -> !esAsistio(r))
                .collect(Collectors.toList());

        int marcados = 0;
        for (RegistroAsistencia asistencia : pendientes) {
            asistencia.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "ASISTIO"));
            asistencia.setFechaCheckIn(LocalDateTime.now());
            asistencia.setMetodoCheckIn("MASIVO");
            asistencia.setIpCheckIn(ip);
            asistenciaRepository.save(asistencia);

            CheckIn checkIn = new CheckIn();
            checkIn.setRegistroAsistencia(asistencia);
            checkIn.setMetodoCheckIn("MASIVO");
            checkIn.setIdUsuario(idOperador);
            checkIn.setIpCheckIn(ip);
            checkInRepository.save(checkIn);
            
            marcados++;
        }

        CheckInMasivoDTO dto = new CheckInMasivoDTO();
        dto.setIdEvento(idEvento);
        dto.setNombreEvento(evento.getNombreEvento());
        dto.setTotalInscritos(registros.size());
        dto.setPendientes(pendientes.size());
        dto.setYaAsistieron(registros.size() - pendientes.size());
        dto.setMarcados(marcados);
        dto.setOmitidos(0);
        dto.setMensaje(String.format("Check-in masivo completado: %d participantes marcados", marcados));
        
        return dto;
    }

    @Override
    @Transactional
    public CheckInDTO anularCheckIn(Long idRegistroAsistencia, Long idOperador, String ip) {
        RegistroAsistencia asistencia = asistenciaRepository.findById(idRegistroAsistencia)
                .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        asistencia.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "REGISTRADO"));
        asistencia.setFechaCheckIn(null);
        asistencia.setMetodoCheckIn("ANULACION");
        asistencia.setIpCheckIn(ip);
        asistenciaRepository.save(asistencia);

        CheckIn checkIn = new CheckIn();
        checkIn.setRegistroAsistencia(asistencia);
        checkIn.setMetodoCheckIn("ANULACION");
        checkIn.setIdUsuario(idOperador);
        checkIn.setIpCheckIn(ip);

        CheckIn guardado = checkInRepository.save(checkIn);
        return mapToDTO(guardado);
    }

    private void validarEstadoEvento(Evento evento) {
        String estado = nombreEstadoEvento(evento);
        if (estado != null && (estado.equalsIgnoreCase("CANCELADO")
                || estado.equalsIgnoreCase("FINALIZADO")
                || estado.equalsIgnoreCase("BORRADOR"))) {
            throw new RuntimeException("No se permite check-in masivo: el evento está " + estado);
        }
    }
}
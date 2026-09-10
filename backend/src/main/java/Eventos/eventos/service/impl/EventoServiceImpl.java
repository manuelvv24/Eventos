package Eventos.eventos.service.impl;

import Eventos.eventos.dto.EventoDTO;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Estado;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.EventoService;
import Eventos.eventos.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventoServiceImpl implements EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EstadoService estadoService;

    @Autowired
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    private String estadoNombre(Evento evento) {
        return evento.getEstado() != null ? evento.getEstado().getNombreEstado() : null;
    }

    private void setEstado(Evento evento, String nombre) {
        Estado estado = estadoService.resolver(EstadoService.TIPO_EVENTO, nombre);
        if (estado == null) {
            throw new RuntimeException("El estado '" + nombre + "' no existe en el catálogo de estados");
        }
        evento.setEstado(estado);
    }

    @Override
    @Transactional
    public List<EventoDTO> obtenerTodos() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Evento> todos = eventoRepository.findAll();
        for (Evento e : todos) {
            verificarYFinalizarSiVencido(e, ahora);
        }
        return todos.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventoDTO obtenerPorId(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("Evento no encontrado con ID: " + id);
        }
        verificarYFinalizarSiVencido(evento, LocalDateTime.now());
        return mapToDTO(evento);
    }

    private void verificarYFinalizarSiVencido(Evento evento, LocalDateTime ahora) {
        String estado = estadoNombre(evento);
        if ("FINALIZADO".equalsIgnoreCase(estado) ||
            "CANCELADO".equalsIgnoreCase(estado)) {
            return;
        }
        LocalDateTime fechaFin = evento.getFechaFinEvento() != null ?
                evento.getFechaFinEvento() : evento.getFechaInicioEvento();
        if (fechaFin != null && fechaFin.isBefore(ahora)) {
            setEstado(evento, "finalizado");
            eventoRepository.save(evento);
        }
    }

    @Override
    @Transactional
    public EventoDTO crear(EventoDTO dto) {
        validarEventoDTO(dto, null);
        Evento evento = mapToEntity(dto);
        if (evento.getEstado() == null) {
            setEstado(evento, "activo");
        }
        if (evento.getEliminado() == null) {
            evento.setEliminado(false);
        }
        Evento guardado = eventoRepository.save(evento);
        return mapToDTO(guardado);
    }

    private void validarEventoDTO(EventoDTO dto, Long idActual) {
        sanitizarTexto(dto);
        if (idActual == null && dto.getFechaInicioEvento() != null &&
                dto.getFechaInicioEvento().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new RuntimeException("La fecha del evento no puede ser anterior a hoy");
        }

        if (dto.getDescripcionEvento() != null && dto.getDescripcionEvento().length() > 2000) {
            throw new RuntimeException("La descripción del evento no puede superar 2000 caracteres");
        }
        if (dto.getNombreEvento() != null && dto.getNombreEvento().length() > 255) {
            throw new RuntimeException("El nombre del evento no puede superar 255 caracteres");
        }

        if (dto.getAforoMaximoEvento() != null && dto.getAforoMaximoEvento() <= 0) {
            throw new RuntimeException("El aforo máximo debe ser mayor a cero");
        }

        long diasEvento = 1;
        if (dto.getFechaInicioEvento() != null && dto.getFechaFinEvento() != null) {
            diasEvento = java.time.temporal.ChronoUnit.DAYS.between(
                    dto.getFechaInicioEvento().toLocalDate(),
                    dto.getFechaFinEvento().toLocalDate()) + 1;
            if (diasEvento < 1) diasEvento = 1;
        }
        if (diasEvento > 1) {
            if (dto.getDiasMinimosCertificacion() == null) {
                throw new RuntimeException("Para eventos de más de 1 día debe indicar días mínimos de asistencia para certificar");
            }
            if (dto.getDiasMinimosCertificacion() < 1) {
                throw new RuntimeException("Los días mínimos de certificación deben ser al menos 1");
            }
            if (dto.getDiasMinimosCertificacion() > diasEvento) {
                throw new RuntimeException("Los días mínimos de certificación (" + dto.getDiasMinimosCertificacion()
                        + ") no pueden superar la duración del evento (" + diasEvento + " días)");
            }
        } else if (dto.getDiasMinimosCertificacion() != null) {
            if (dto.getDiasMinimosCertificacion() < 1) {
                throw new RuntimeException("Los días mínimos de certificación deben ser al menos 1");
            }
            if (dto.getDiasMinimosCertificacion() > 1) {
                throw new RuntimeException("Para eventos de 1 día los días mínimos no pueden superar 1");
            }
        }

        String mod = dto.getModalidadEvento() != null ? dto.getModalidadEvento().toLowerCase() : "";
        boolean esVirtual = mod.contains("virtual");
        boolean esPresencial = mod.contains("presencial");
        boolean esHibrido = mod.contains("hibrido") || mod.contains("híbrido");
        if (esVirtual && !esHibrido) {
            if (dto.getEnlaceUrl() == null || dto.getEnlaceUrl().isBlank())
                throw new RuntimeException("Para modalidad VIRTUAL la URL es obligatoria");
            if (dto.getLugarEvento() != null && !dto.getLugarEvento().isBlank())
                dto.setLugarEvento(null); // no mezclar, virtual solo URL
        } else if (esPresencial && !esHibrido) {
            if (dto.getLugarEvento() == null || dto.getLugarEvento().isBlank())
                throw new RuntimeException("Para modalidad PRESENCIAL el lugar es obligatorio");
            if (dto.getEnlaceUrl() != null && !dto.getEnlaceUrl().isBlank())
                dto.setEnlaceUrl(null);
        } else if (esHibrido) {
            if (dto.getLugarEvento() == null || dto.getLugarEvento().isBlank())
                throw new RuntimeException("Para modalidad HÍBRIDO el lugar es obligatorio");
            if (dto.getEnlaceUrl() == null || dto.getEnlaceUrl().isBlank())
                throw new RuntimeException("Para modalidad HÍBRIDO la URL es obligatoria");
        }

        if (dto.getNombreEvento() != null && dto.getFechaInicioEvento() != null) {
            java.time.LocalDate fecha = dto.getFechaInicioEvento().toLocalDate();
            boolean existeDuplicado = eventoRepository.existsCadena(
                    dto.getNombreEvento().toLowerCase(),
                    fecha.atStartOfDay(),
                    fecha.atStartOfDay().plusDays(1),
                    idActual);
            if (existeDuplicado) {
                throw new RuntimeException("Ya existe un evento con nombre y fecha similares.");
            }
        }
    }

    /** Limpia los campos de texto libre para evitar inserciones maliciosas (XSS/HTML/scripts). */
    private void sanitizarTexto(EventoDTO dto) {
        dto.setNombreEvento(limpiar(dto.getNombreEvento()));
        dto.setDescripcionEvento(limpiar(dto.getDescripcionEvento()));
        dto.setLugarEvento(limpiar(dto.getLugarEvento()));
        dto.setTipoEvento(limpiar(dto.getTipoEvento()));
        dto.setModalidadEvento(limpiar(dto.getModalidadEvento()));
    }

    private String limpiar(String valor) {
        if (valor == null) return null;
        return valor
                .replaceAll("(?is)<\\s*script\\b.*?<\\s*/\\s*script\\s*>", "")
                .replaceAll("<!-{2,}", "")
                .replaceAll("(?i)javascript\\s*:", "")
                .replaceAll("(?i)\\bon\\w+\\s*=\\s*('([^']*)'|\"([^\"]*)\"|[^\\s>]+)", "")
                .replaceAll("[<>]", "")
                .trim();
    }

    @Override
    @Transactional
    public EventoDTO actualizar(Long id, EventoDTO dto) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("No se puede editar un evento que está en la papelera.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaFinOriginal = evento.getFechaFinEvento() != null ?
                evento.getFechaFinEvento() : evento.getFechaInicioEvento();

        if ("FINALIZADO".equalsIgnoreCase(estadoNombre(evento)) ||
            (fechaFinOriginal != null && fechaFinOriginal.isBefore(ahora))) {
            if (!"FINALIZADO".equalsIgnoreCase(estadoNombre(evento))) {
                setEstado(evento, "finalizado");
                eventoRepository.save(evento);
            }
            throw new RuntimeException("No se puede editar un evento que ya ha finalizado.");
        }

        validarEventoDTO(dto, id);

        String estadoAnterior = estadoNombre(evento);
        LocalDateTime fechaAnterior = evento.getFechaInicioEvento();
        String lugarAnterior = evento.getLugarEvento();

        evento.setNombreEvento(dto.getNombreEvento());
        evento.setDescripcionEvento(dto.getDescripcionEvento());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setModalidadEvento(dto.getModalidadEvento());
        evento.setFechaInicioEvento(dto.getFechaInicioEvento());
        evento.setFechaFinEvento(dto.getFechaFinEvento());
        evento.setLugarEvento(dto.getLugarEvento());
        evento.setAforoMaximoEvento(dto.getAforoMaximoEvento());
        evento.setDuracionEvento(dto.getDuracionEvento());
        evento.setDiasMinimosCertificacion(dto.getDiasMinimosCertificacion());
        if (dto.getEstadoEvento() != null) {
            setEstado(evento, dto.getEstadoEvento());
        }
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setEnlaceUrl(dto.getEnlaceUrl());
        evento.setFechaLimiteInscripcion(dto.getFechaLimiteInscripcion());

        Evento guardado = eventoRepository.save(evento);

        boolean fueCancelado = "CANCELADO".equalsIgnoreCase(dto.getEstadoEvento())
                && !"CANCELADO".equalsIgnoreCase(estadoAnterior);

        if (fueCancelado) {
            notificacionService.notificarInscritos(
                    evento,
                    "CANCELACION",
                    "Evento cancelado",
                    "El evento \"" + evento.getNombreEvento() + "\" fue cancelado. Lamentamos los inconvenientes."
            );
        }

        List<String> cambios = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (fechaAnterior != null && guardado.getFechaInicioEvento() != null
                && !fechaAnterior.equals(guardado.getFechaInicioEvento())) {
            cambios.add("Fecha: " + fechaAnterior.format(fmt) + " -> " + guardado.getFechaInicioEvento().format(fmt));
        }
        if (lugarAnterior != null && !lugarAnterior.equals(guardado.getLugarEvento())) {
            cambios.add("Lugar: " + lugarAnterior + " -> " + guardado.getLugarEvento());
        }

        if (!fueCancelado) {
            String mensajeCambios = cambios.isEmpty()
                    ? "El evento \"" + guardado.getNombreEvento() + "\" ha sido actualizado. Por favor revisa los detalles."
                    : "El evento \"" + guardado.getNombreEvento() + "\" tuvo cambios. " + String.join(" · ", cambios) + ".";
            notificacionService.notificarInscritos(
                    guardado,
                    "ACTUALIZACION",
                    "Evento actualizado",
                    mensajeCambios
            );
        }
        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public EventoDTO cancelarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("No se puede cancelar un evento que está en la papelera.");
        }

        if ("CANCELADO".equalsIgnoreCase(estadoNombre(evento))) {
            throw new RuntimeException("El evento ya está inhabilitado/cancelado.");
        }

        setEstado(evento, "cancelado");
        Evento guardado = eventoRepository.save(evento);

        try {
            notificacionService.notificarInscritos(
                    guardado,
                    "CANCELACION",
                    "Evento cancelado",
                    "El evento \"" + guardado.getNombreEvento() + "\" fue cancelado. Lamentamos los inconvenientes."
            );
        } catch (Exception e) {
            System.err.println("[WARN] Error al notificar inscritos: " + e.getMessage());
        }

        return mapToDTO(guardado);
    }

    @Override
    @Transactional
    public void eliminarLogico(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException("El evento ya está en la papelera.");
        }
        evento.setEliminado(true);
        eventoRepository.save(evento);
    }

    @Override
    @Transactional
    public EventoDTO restaurar(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
        evento.setEliminado(false);
        return mapToDTO(eventoRepository.save(evento));
    }

    @Override
    public List<EventoDTO> obtenerEliminados() {
        return eventoRepository.findByEliminadoTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private EventoDTO mapToDTO(Evento evento) {
        EventoDTO dto = new EventoDTO();
        dto.setIdEventos(evento.getIdEventos());
        dto.setNombreEvento(evento.getNombreEvento());
        dto.setDescripcionEvento(evento.getDescripcionEvento());
        dto.setTipoEvento(evento.getTipoEvento());
        dto.setModalidadEvento(evento.getModalidadEvento());
        dto.setFechaInicioEvento(evento.getFechaInicioEvento());
        dto.setFechaFinEvento(evento.getFechaFinEvento());
        dto.setLugarEvento(evento.getLugarEvento());
        dto.setAforoMaximoEvento(evento.getAforoMaximoEvento());
        dto.setInscritos((int) registroAsistenciaRepository.countInscritosPorEvento(evento.getIdEventos()));
        dto.setDuracionEvento(evento.getDuracionEvento());
        dto.setDiasMinimosCertificacion(evento.getDiasMinimosCertificacion());
        dto.setEstadoEvento(estadoNombre(evento));
        dto.setImagenUrl(evento.getImagenUrl());
        dto.setEnlaceUrl(evento.getEnlaceUrl());
        dto.setFechaLimiteInscripcion(evento.getFechaLimiteInscripcion());
        dto.setEliminado(evento.getEliminado() != null ? evento.getEliminado() : false);
        return dto;
    }

    private Evento mapToEntity(EventoDTO dto) {
        Evento evento = new Evento();
        evento.setNombreEvento(dto.getNombreEvento());
        evento.setDescripcionEvento(dto.getDescripcionEvento());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setModalidadEvento(dto.getModalidadEvento());
        evento.setFechaInicioEvento(dto.getFechaInicioEvento());
        evento.setFechaFinEvento(dto.getFechaFinEvento());
        evento.setLugarEvento(dto.getLugarEvento());
        evento.setAforoMaximoEvento(dto.getAforoMaximoEvento());
        evento.setDuracionEvento(dto.getDuracionEvento());
        evento.setDiasMinimosCertificacion(dto.getDiasMinimosCertificacion());
        if (dto.getEstadoEvento() != null) {
            setEstado(evento, dto.getEstadoEvento());
        }
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setEnlaceUrl(dto.getEnlaceUrl());
        evento.setFechaLimiteInscripcion(dto.getFechaLimiteInscripcion());
        evento.setEliminado(dto.getEliminado() != null ? dto.getEliminado() : false);
        return evento;
    }
}
package Eventos.eventos.service;

import Eventos.eventos.dto.MiEventoDTO;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MisEventosService {

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private RegistroAsistenciaRepository registroRepository;

    @Transactional(readOnly = true)
    public List<MiEventoDTO> misEventos(Long idUsuario) {
        List<MiEventoDTO> resultado = new ArrayList<>();

        for (Participante p : participanteRepository.findAllByUsuario_IdUsuario(idUsuario)) {
            for (RegistroAsistencia r : registroRepository.findByParticipante_IdParticipante(p.getIdParticipante())) {
                // Si la persona ya asistió (se escaneó el QR en check-in), no mostrar el evento en "Mis Eventos"
                String estadoRegistro = r.getEstado() != null ? r.getEstado().getNombreEstado() : null;
                if ("ASISTIO".equalsIgnoreCase(estadoRegistro) || r.estaEliminado()) {
                    continue;
                }
                Evento e = r.getEvento();
                MiEventoDTO dto = new MiEventoDTO();
                dto.setIdRegistro(r.getIdRegistroAsistencia());
                dto.setIdEvento(e.getIdEventos());
                dto.setNombreEvento(e.getNombreEvento());
                dto.setTipoEvento(e.getTipoEvento());
                dto.setModalidadEvento(e.getModalidadEvento());
                dto.setFechaInicioEvento(e.getFechaInicioEvento());
                dto.setLugarEvento(e.getLugarEvento());
                dto.setEstadoEvento(e.getEstado() != null ? e.getEstado().getNombreEstado() : null);
                dto.setEstadoAsistencia(estadoRegistro);
                dto.setCodigoQr(r.getCodigoQrInscripcion());
                resultado.add(dto);
            }
        }
        return resultado;
    }
}
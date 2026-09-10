package Eventos.eventos.service.impl;

import Eventos.eventos.dto.ParticipanteDto;
import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.UsuarioRepository;
import Eventos.eventos.service.ParticipanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParticipanteServiceImpl implements ParticipanteService {

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public ParticipanteDto registrar(ParticipanteDto dto) {
        Usuario usuario = usuarioRepository.findById(dto.getIdUsuarios())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.getIdUsuarios()));
        Participante participante = new Participante();
        participante.setUsuario(usuario);
        Participante guardado = participanteRepository.save(participante);
        return mapToDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipanteDto> obtenerTodos() {
        return participanteRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipanteDto obtenerPorId(Long id) {
        Participante participante = participanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Participante no encontrado con ID: " + id));
        return mapToDto(participante);
    }

    private ParticipanteDto mapToDto(Participante participante) {
        ParticipanteDto dto = new ParticipanteDto();
        dto.setIdParticipantes(participante.getIdParticipante());
        dto.setIdUsuarios(participante.getUsuario() != null
                ? participante.getUsuario().getIdUsuario() : null);
        return dto;
    }
}

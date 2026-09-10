package Eventos.eventos.service.impl;

import Eventos.eventos.dto.ImportacionDTO.ParticipanteImportDTO;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Login;
import Eventos.eventos.entity.Participante;
import Eventos.eventos.entity.RegistroAsistencia;
import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.repository.UsuarioRepository;
import Eventos.eventos.service.EstadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
public class ParticipanteImportWorker {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LoginRepository loginRepository;
    @Autowired private ParticipanteRepository participanteRepository;
    @Autowired private RegistroAsistenciaRepository asistenciaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EstadoService estadoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarFila(ParticipanteImportDTO fila, Rol rol, Evento evento) {
        Usuario usuario = obtenerOCrearUsuario(fila, rol);
        Participante participante = obtenerOCrearParticipante(usuario);
        registrarEnEvento(participante, evento);
    }

    private Usuario obtenerOCrearUsuario(ParticipanteImportDTO fila, Rol rol) {
        return loginRepository.findByEmailUsuario(fila.getEmail())
                .map(Login::getUsuario)
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setPrimerNombreUsuario(fila.getPrimerNombre());
                    u.setSegundoNombreUsuario(fila.getSegundoNombre());
                    u.setPrimerApellidoUsuario(fila.getPrimerApellido());
                    u.setSegundoApellidoUsuario(fila.getSegundoApellido());
                    u.setTipoDocumentoUsuario(fila.getTipoDocumento());
                    u.setNumeroDocumentoUsuario(fila.getNumeroDocumento());
                    u.setNumeroTelefonoUsuario(fila.getTelefono());
                    u.setRoles(Set.of(rol));
                    u.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO, "Activo"));
                    u = usuarioRepository.save(u);

                    Login l = new Login();
                    l.setUsuario(u);
                    l.setEmailUsuario(fila.getEmail());
                    l.setContrasenaUsuario(passwordEncoder.encode(fila.getNumeroDocumento()));
                    l.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO));
                    loginRepository.save(l);
                    return u;
                });
    }

    private Participante obtenerOCrearParticipante(Usuario usuario) {
        return participanteRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseGet(() -> {
                    Participante p = new Participante();
                    p.setUsuario(usuario);
                    return participanteRepository.save(p);
                });
    }

    private void registrarEnEvento(Participante participante, Evento evento) {
        RegistroAsistencia registro = asistenciaRepository
                .findByEvento_IdEventosAndParticipante_IdParticipante(
                        evento.getIdEventos(), participante.getIdParticipante())
                .filter(r -> r.estaEliminado())
                .orElseGet(() -> {
                    RegistroAsistencia nuevo = new RegistroAsistencia();
                    nuevo.setEvento(evento);
                    nuevo.setParticipante(participante);
                    nuevo.setFechaAsistencia(LocalDateTime.now());
                    nuevo.setCodigoQrInscripcion("QR-" + UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 10).toUpperCase());
                    return nuevo;
                });

        if (registro.getEstado() == null || registro.estaEliminado()) {
            registro.setEstado(estadoService.resolver(EstadoService.TIPO_REGISTRO, "REGISTRADO"));
            asistenciaRepository.save(registro);
        }
    }
}

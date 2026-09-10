package Eventos.eventos.service.impl;

import Eventos.eventos.dto.UsuarioDTO;
import Eventos.eventos.entity.Login;
import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.RolRepository;
import Eventos.eventos.repository.UsuarioRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.UsuarioService;
import Eventos.eventos.util.TipoDocumentoUtils;
import Eventos.eventos.util.RolNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EstadoService estadoService;

    private boolean tieneRol(Usuario usuario, String... nombres) {
        if (usuario.getRoles() == null) return false;
        for (Rol r : usuario.getRoles()) {
            for (String nombre : nombres) {
                if (nombre.equalsIgnoreCase(r.getNombreRol())) return true;
            }
        }
        return false;
    }

    private void asignarRoles(Usuario usuario, List<Long> idsRoles) {
        if (idsRoles == null || idsRoles.isEmpty()) {
            throw new RuntimeException("Debe asignar al menos un rol");
        }
        Set<Rol> roles = idsRoles.stream().distinct().map(id ->
                        rolRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + id)))
                .collect(Collectors.toSet());
        usuario.setRoles(roles);
    }

    private boolean esSuperAdminEnSesion() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + RolNames.SUPER_ADMIN).equalsIgnoreCase(a.getAuthority()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .filter(u -> !u.estaEliminado())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarEliminados() {
        return usuarioRepository.findAll().stream()
                .filter(Usuario::estaEliminado)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPorId(Long id) {
        return toDTO(usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id)));
    }

    @Override
    @Transactional
    public UsuarioDTO registrarUsuario(UsuarioDTO dto) {
        if (dto.getEmailUsuario() == null || dto.getEmailUsuario().isBlank()) {
            throw new RuntimeException("El correo electrónico es obligatorio");
        }
        String email = dto.getEmailUsuario().trim().toLowerCase();
        if (loginRepository.existsByEmailUsuario(email)) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }
        if (dto.getNumeroDocumentoUsuario() != null
                && usuarioRepository.existsByNumeroDocumentoUsuario(dto.getNumeroDocumentoUsuario().trim())) {
            throw new RuntimeException("El número de documento ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setPrimerNombreUsuario(dto.getPrimerNombreUsuario());
        usuario.setSegundoNombreUsuario(dto.getSegundoNombreUsuario());
        usuario.setPrimerApellidoUsuario(dto.getPrimerApellidoUsuario());
        usuario.setSegundoApellidoUsuario(dto.getSegundoApellidoUsuario());
        usuario.setTipoDocumentoUsuario(normalizarTipoDocumento(dto.getTipoDocumentoUsuario()));
        usuario.setNumeroDocumentoUsuario(dto.getNumeroDocumentoUsuario());
        usuario.setNumeroTelefonoUsuario(dto.getNumeroTelefonoUsuario());
        boolean estadoPerfil = dto.getEstadoUsuario() == null || dto.getEstadoUsuario();
        usuario.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO,
                estadoPerfil ? Login.ESTADO_ACTIVO : Login.ESTADO_INACTIVO));

        if (dto.getIdsRoles() != null && !dto.getIdsRoles().isEmpty()) {
            asignarRoles(usuario, dto.getIdsRoles());
        } else if (dto.getIdRol() != null) {
            asignarRoles(usuario, List.of(dto.getIdRol()));
        } else {
            Rol rolDefault = rolRepository.findByNombreRolIgnoreCase(RolNames.INVITADO)
                    .orElseThrow(() -> new RuntimeException("Rol por defecto no encontrado"));
            asignarRoles(usuario, List.of(rolDefault.getIdRol()));
        }

        Usuario guardado = usuarioRepository.save(usuario);

        Login login = new Login();
        login.setUsuario(guardado);
        login.setEmailUsuario(email);
        login.setContrasenaUsuario(passwordEncoder.encode(
                (dto.getContrasenaUsuario() != null && !dto.getContrasenaUsuario().isEmpty())
                        ? dto.getContrasenaUsuario()
                        : (dto.getNumeroDocumentoUsuario() != null ? dto.getNumeroDocumentoUsuario() : "Cambiar123*")));
        login.setEstado(dto.getEstadoUsuario() == null || dto.getEstadoUsuario()
                ? estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO)
                : estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_INACTIVO));
        loginRepository.save(login);

        return toDTO(guardado);
    }

    @Override
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setPrimerNombreUsuario(dto.getPrimerNombreUsuario());
        usuario.setSegundoNombreUsuario(dto.getSegundoNombreUsuario());
        usuario.setPrimerApellidoUsuario(dto.getPrimerApellidoUsuario());
        usuario.setSegundoApellidoUsuario(dto.getSegundoApellidoUsuario());
        usuario.setTipoDocumentoUsuario(normalizarTipoDocumento(dto.getTipoDocumentoUsuario()));
        usuario.setNumeroDocumentoUsuario(dto.getNumeroDocumentoUsuario());
        usuario.setNumeroTelefonoUsuario(dto.getNumeroTelefonoUsuario());

        boolean estadoPerfil = dto.getEstadoUsuario() == null || dto.getEstadoUsuario();
        usuario.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO,
                estadoPerfil ? Login.ESTADO_ACTIVO : Login.ESTADO_INACTIVO));

        Login login = usuario.getLogin();
        if (login == null) {
            login = new Login();
            login.setUsuario(usuario);
            login.setEmailUsuario(dto.getEmailUsuario() != null && !dto.getEmailUsuario().isBlank()
                    ? dto.getEmailUsuario().trim().toLowerCase()
                    : "usuario" + usuario.getIdUsuario() + "@kineticpulse.com");
            login.setContrasenaUsuario(passwordEncoder.encode(
                    (dto.getContrasenaUsuario() != null && !dto.getContrasenaUsuario().isEmpty())
                            ? dto.getContrasenaUsuario()
                            : (dto.getNumeroDocumentoUsuario() != null ? dto.getNumeroDocumentoUsuario() : "Cambiar123*")));
        } else {
            if (dto.getEmailUsuario() != null && !dto.getEmailUsuario().isBlank()) {
                login.setEmailUsuario(dto.getEmailUsuario().trim().toLowerCase());
            }
            if (dto.getContrasenaUsuario() != null && !dto.getContrasenaUsuario().isEmpty()) {
                login.setContrasenaUsuario(passwordEncoder.encode(dto.getContrasenaUsuario()));
            }
        }
        login.setEstado(estadoPerfil
                ? estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO)
                : estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_INACTIVO));
        if (estadoPerfil) {
            login.setIntentosFallidos(0);
            login.setFechaBloqueo(null);
        }
        loginRepository.save(login);

        List<Long> idsNuevos = (dto.getIdsRoles() != null && !dto.getIdsRoles().isEmpty())
                ? dto.getIdsRoles()
                : (dto.getIdRol() != null ? List.of(dto.getIdRol()) : null);

        if (idsNuevos != null) {
            if (tieneRol(usuario, RolNames.SUPER_ADMIN) && !esSuperAdminEnSesion()) {
                throw new RuntimeException("Solo el Super Administrador puede modificar a otro Super Administrador.");
            }

            List<Rol> rolesNuevos = idsNuevos.stream().distinct()
                    .map(idRol -> rolRepository.findById(idRol)
                            .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + idRol)))
                    .collect(Collectors.toList());

            boolean esAdminActual = tieneRol(usuario, RolNames.ADMIN, RolNames.SUPER_ADMIN);
            boolean seraAdmin = rolesNuevos.stream().anyMatch(r ->
                    RolNames.ADMIN.equalsIgnoreCase(r.getNombreRol())
                            || RolNames.SUPER_ADMIN.equalsIgnoreCase(r.getNombreRol()));

            if (esAdminActual && !seraAdmin) {
                long totalAdmins = contarAdministradores();
                if (totalAdmins <= 1) {
                    throw new RuntimeException("Debe existir al menos un Administrador en el sistema. No se puede cambiar el rol.");
                }
            }
            asignarRoles(usuario, idsNuevos);
        }

        return toDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        if (usuario.estaEliminado()) {
            throw new RuntimeException("El usuario ya está eliminado.");
        }
        if (tieneRol(usuario, RolNames.SUPER_ADMIN) && !esSuperAdminEnSesion()) {
            throw new RuntimeException("Solo el Super Administrador puede eliminar a otro Super Administrador.");
        }
        boolean esAdmin = tieneRol(usuario, RolNames.ADMIN, RolNames.SUPER_ADMIN);
        if (esAdmin) {
            long totalAdmins = contarAdministradores();
            if (totalAdmins <= 1) {
                throw new RuntimeException("Debe existir al menos un Administrador en el sistema. No se puede eliminar a este usuario.");
            }
        }
        usuario.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO, Login.ESTADO_ELIMINADO));
        usuarioRepository.save(usuario);

        loginRepository.findByUsuario_IdUsuario(id).ifPresent(login -> {
            login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ELIMINADO));
            loginRepository.save(login);
        });
    }

    @Override
    @Transactional
    public UsuarioDTO restaurarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        if (!usuario.estaEliminado()) {
            throw new RuntimeException("El usuario no está eliminado.");
        }
        usuario.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO, Login.ESTADO_ACTIVO));
        usuarioRepository.save(usuario);

        loginRepository.findByUsuario_IdUsuario(id).ifPresent(login -> {
            login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO));
            login.setIntentosFallidos(0);
            login.setUltimoIntentoFallido(null);
            login.setFechaBloqueo(null);
            loginRepository.save(login);
        });
        return toDTO(usuario);
    }

    private long contarAdministradores() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() != null
                        && Login.ESTADO_ACTIVO.equalsIgnoreCase(u.getEstado().getNombreEstado())
                        && tieneRol(u, RolNames.ADMIN, RolNames.SUPER_ADMIN))
                .count();
    }

    private UsuarioDTO toDTO(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setIdUsuario(u.getIdUsuario());
        dto.setPrimerNombreUsuario(u.getPrimerNombreUsuario());
        dto.setSegundoNombreUsuario(u.getSegundoNombreUsuario());
        dto.setPrimerApellidoUsuario(u.getPrimerApellidoUsuario());
        dto.setSegundoApellidoUsuario(u.getSegundoApellidoUsuario());
        dto.setEmailUsuario(u.getLogin() != null ? u.getLogin().getEmailUsuario() : null);
        dto.setTipoDocumentoUsuario(u.getTipoDocumentoUsuario());
        dto.setNumeroDocumentoUsuario(u.getNumeroDocumentoUsuario());
        dto.setNumeroTelefonoUsuario(u.getNumeroTelefonoUsuario());
        dto.setEstadoUsuario(u.getEstado() != null
                && Login.ESTADO_ACTIVO.equalsIgnoreCase(u.getEstado().getNombreEstado()));
        if (u.getRoles() != null && !u.getRoles().isEmpty()) {
            List<Rol> roles = u.getRoles().stream()
                    .sorted((a, b) -> rankRol(a.getNombreRol()) - rankRol(b.getNombreRol()))
                    .collect(Collectors.toList());
            dto.setIdsRoles(roles.stream().map(Rol::getIdRol).collect(Collectors.toList()));
            dto.setNombresRoles(roles.stream().map(Rol::getNombreRol).collect(Collectors.toList()));
            dto.setIdRol(roles.get(0).getIdRol());
            dto.setRolNombre(RolNames.primario(u.getRoles()));
        }
        return dto;
    }

    private int rankRol(String nombre) {
        if (RolNames.SUPER_ADMIN.equalsIgnoreCase(nombre)) return 0;
        if (RolNames.ADMIN.equalsIgnoreCase(nombre)) return 1;
        if (RolNames.OPERADOR.equalsIgnoreCase(nombre)) return 2;
        if (RolNames.MONITOR.equalsIgnoreCase(nombre)) return 3;
        return 4;
    }

    private String normalizarTipoDocumento(String tipo) {
        String normalizado = TipoDocumentoUtils.normalizar(tipo);
        if (normalizado == null) {
            throw new RuntimeException("El tipo de documento no es válido");
        }
        return normalizado;
    }
}
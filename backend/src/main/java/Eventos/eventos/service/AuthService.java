package Eventos.eventos.service;

import Eventos.eventos.audit.SeguridadUtils;
import Eventos.eventos.config.JwtUtil;
import Eventos.eventos.dto.AuthResponseDTO;
import Eventos.eventos.dto.LoginDTO;
import Eventos.eventos.dto.RegistroDTO;
import Eventos.eventos.entity.Login;
import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.exeption.DuplicadoException;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.RolRepository;
import Eventos.eventos.repository.UsuarioRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.util.TipoDocumentoUtils;
import Eventos.eventos.util.RolNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int MAX_INTENTOS = 5;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private EstadoService estadoService;

    @Autowired
    private EmailService emailService;

    private static final int MINUTOS_CODIGO_RECUPERACION = 15;

    private String primerRol(Usuario usuario) {
        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            return null;
        }
        return RolNames.primario(usuario.getRoles());
    }

    // ── Sanitización de datos de registro ─────────────────────────────────────

    private String normalizarNombre(String valor) {
        if (valor == null) return null;
        return valor.trim().replaceAll("\\s+", " ");
    }

    private String normalizarDocumento(String valor) {
        if (valor == null) return null;
        return valor.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private String normalizarEmail(String valor) {
        if (valor == null) return null;
        return valor.trim().toLowerCase();
    }

    private String ipActual() {
        return SeguridadUtils.ipActual();
    }

    @Transactional
    public AuthResponseDTO registrarUsuario(RegistroDTO dto) {
        String email       = normalizarEmail(dto.getEmail());
        String documento   = normalizarDocumento(dto.getNumeroDocumento());
        dto.setPrimerNombre(normalizarNombre(dto.getPrimerNombre()));
        dto.setSegundoNombre(normalizarNombre(dto.getSegundoNombre()));
        dto.setPrimerApellido(normalizarNombre(dto.getPrimerApellido()));
        dto.setSegundoApellido(normalizarNombre(dto.getSegundoApellido()));

        if (loginRepository.existsByEmailUsuario(email)) {
            throw new DuplicadoException("El correo electrónico ya está registrado", "email");
        }
        if (usuarioRepository.existsByNumeroDocumentoUsuario(documento)) {
            throw new DuplicadoException("El número de documento ya está registrado", "numeroDocumento");
        }
        Rol rolDefault = rolRepository.findByNombreRolIgnoreCase(RolNames.INVITADO)
                .orElseThrow(() -> new RuntimeException("No se encontró ningún rol en la base de datos"));

        Usuario usuario = new Usuario();
        usuario.setPrimerNombreUsuario(dto.getPrimerNombre());
        usuario.setSegundoNombreUsuario(dto.getSegundoNombre());
        usuario.setPrimerApellidoUsuario(dto.getPrimerApellido());
        usuario.setSegundoApellidoUsuario(dto.getSegundoApellido());
        usuario.setTipoDocumentoUsuario(normalizarTipoDocumento(dto.getTipoDocumento()));
        usuario.setNumeroDocumentoUsuario(documento);
        usuario.setNumeroTelefonoUsuario(dto.getTelefono() != null && !dto.getTelefono().isBlank()
                ? dto.getTelefono().replaceAll("[^0-9]", "") : null);
        usuario.setRoles(Set.of(rolDefault));
        usuario.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO,
                EstadoService.capitalizar(Login.ESTADO_ACTIVO)));

        Usuario guardado = usuarioRepository.save(usuario);

        Login login = new Login();
        login.setUsuario(guardado);
        login.setEmailUsuario(email);
        login.setEmailRecuperacion(dto.getEmailRecuperacion());
        login.setContrasenaUsuario(passwordEncoder.encode(dto.getPassword()));
        login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO));
        loginRepository.save(login);

        String token = jwtUtil.generarToken(
                login.getEmailUsuario(),
                guardado.getIdUsuario(),
                primerRol(guardado)
        );

        String nombreCompleto = construirNombreCompleto(guardado);
        List<String> roles = nombresRoles(guardado.getRoles());
        return new AuthResponseDTO("Usuario registrado exitosamente", guardado.getIdUsuario(),
                login.getEmailUsuario(), nombreCompleto, primerRol(guardado), token, roles);
    }

    @Transactional
    public AuthResponseDTO login(LoginDTO dto) {
        String email = normalizarEmail(dto.getEmail());
        String ip = ipActual();

        Login login = loginRepository.findByEmailUsuario(email).orElse(null);
        if (login == null) {
            auditoriaService.registrar("LOGIN_FALLIDO", "LOGIN", null,
                    "Intento de login con correo no registrado: " + email, null, ip);
            throw new RuntimeException("El correo electrónico no está registrado");
        }

        Usuario usuario = login.getUsuario();
        boolean perfilInactivo = usuario.getEstado() != null
                && !Login.ESTADO_ACTIVO.equalsIgnoreCase(usuario.getEstado().getNombreEstado());
        boolean loginDeshabilitado = login.getEstado() != null
                && !Login.ESTADO_ACTIVO.equalsIgnoreCase(login.getEstado().getNombreEstado());

        if (perfilInactivo || loginDeshabilitado) {
            auditoriaService.registrar("LOGIN_BLOQUEADO", "LOGIN", usuario.getIdUsuario(),
                    perfilInactivo
                            ? "Intento de login en cuenta desactivada"
                            : "Intento de login en cuenta bloqueada: " + estadoService.nombre(login.getEstado()),
                    usuario.getIdUsuario(), ip);
            throw new RuntimeException("La cuenta está desactivada. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), login.getContrasenaUsuario())) {
            int intentos = (login.getIntentosFallidos() != null ? login.getIntentosFallidos() : 0) + 1;
            login.setIntentosFallidos(intentos);
            login.setUltimoIntentoFallido(LocalDateTime.now());

            if (intentos >= MAX_INTENTOS) {
                login.setFechaBloqueo(LocalDateTime.now());
                login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_BLOQUEADO));
                loginRepository.save(login);
                auditoriaService.registrar("CUENTA_BLOQUEADA", "LOGIN", usuario.getIdUsuario(),
                        "Cuenta bloqueada automáticamente tras " + MAX_INTENTOS + " intentos fallidos",
                        usuario.getIdUsuario(), ip);
                throw new RuntimeException("Cuenta bloqueada por " + MAX_INTENTOS
                        + " intentos fallidos consecutivos. Contacte al administrador.");
            } else {
                loginRepository.save(login);
                auditoriaService.registrar("LOGIN_FALLIDO", "LOGIN", usuario.getIdUsuario(),
                        "Contraseña incorrecta (Intento " + intentos + " de " + MAX_INTENTOS + ")",
                        usuario.getIdUsuario(), ip);
                throw new RuntimeException("La contraseña es incorrecta");
            }
        }

        login.setIntentosFallidos(0);
        login.setUltimoIntentoFallido(null);
        login.setFechaBloqueo(null);
        login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO));
        loginRepository.save(login);

        boolean recordarme = Boolean.TRUE.equals(dto.getRecordarme());
        String token = jwtUtil.generarToken(
                login.getEmailUsuario(),
                usuario.getIdUsuario(),
                primerRol(usuario),
                recordarme
        );

        auditoriaService.registrar("LOGIN_EXITOSO", "LOGIN", usuario.getIdUsuario(),
                "Inicio de sesión exitoso (Recordarme: " + recordarme + ")", usuario.getIdUsuario(), ip);

        String nombreCompleto = construirNombreCompleto(usuario);
        List<String> roles = nombresRoles(usuario.getRoles());
        return new AuthResponseDTO("Login exitoso", usuario.getIdUsuario(),
                login.getEmailUsuario(), nombreCompleto, primerRol(usuario), token, roles);
    }

    // ── Cambio de rol en sesión (sin re-ingresar credenciales) ──────────────

    /** Cierra la sesión actual re-emitiendo el token con el rol elegido por el usuario. */
    public AuthResponseDTO cambiarRol(String token, String rolSolicitado) {
        if (token == null || token.isBlank() || !jwtUtil.esTokenValido(token)) {
            throw new RuntimeException("Sesión inválida o expirada");
        }
        Long idUsuario = jwtUtil.extraerIdUsuario(token);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String rolCanonico = resolverRolNormalizado(usuario.getRoles(), rolSolicitado);
        if (rolCanonico == null) {
            throw new RuntimeException("No tienes asignado el rol solicitado");
        }

        String nuevoToken = jwtUtil.generarToken(
                usuario.getLogin().getEmailUsuario(), usuario.getIdUsuario(), rolCanonico);

        auditoriaService.registrar("ROL_CAMBIADO", "USUARIO", usuario.getIdUsuario(),
                "Cambió de rol en sesión a: " + rolCanonico, usuario.getIdUsuario(), ipActual());

        String nombreCompleto = construirNombreCompleto(usuario);
        return new AuthResponseDTO("Rol cambiado a " + rolCanonico, usuario.getIdUsuario(),
                usuario.getLogin().getEmailUsuario(), nombreCompleto, rolCanonico, nuevoToken,
                nombresRoles(usuario.getRoles()));
    }

    /** Devuelve los nombres canónicos de los roles del usuario. */
    private List<String> nombresRoles(Set<Rol> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .map(Rol::getNombreRol)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .sorted((a, b) -> indicePrioridad(a) - indicePrioridad(b))
                .collect(Collectors.toList());
    }

    private int indicePrioridad(String rol) {
        if (RolNames.SUPER_ADMIN.equalsIgnoreCase(rol)) return 0;
        if (RolNames.ADMIN.equalsIgnoreCase(rol)) return 1;
        if (RolNames.OPERADOR.equalsIgnoreCase(rol)) return 2;
        if (RolNames.MONITOR.equalsIgnoreCase(rol)) return 3;
        return 4;
    }

    /** Busca en los roles del usuario el solicitado (insensible a mayúsculas) y devuelve su nombre canónico. */
    private String resolverRolNormalizado(Set<Rol> roles, String solicitado) {
        if (roles == null || solicitado == null || solicitado.isBlank()) return null;
        for (Rol r : roles) {
            if (r.getNombreRol() != null && r.getNombreRol().equalsIgnoreCase(solicitado)) {
                return r.getNombreRol();
            }
        }
        return null;
    }

    // ── Recuperación de cuenta (código de 6 dígitos) ─────────────────────────

    /** Comprueba en vivo si un correo y/o documento ya están registrados. true = disponible. */
    public Map<String, Boolean> verificarDisponibilidad(String email, String numeroDocumento) {
        Map<String, Boolean> resultado = new HashMap<>();
        if (email != null && !email.isBlank()) {
            resultado.put("email", !loginRepository.existsByEmailUsuario(normalizarEmail(email)));
        }
        if (numeroDocumento != null && !numeroDocumento.isBlank()) {
            resultado.put("numeroDocumento",
                    !usuarioRepository.existsByNumeroDocumentoUsuario(normalizarDocumento(numeroDocumento)));
        }
        return resultado;
    }

    /** Busca la cuenta por correo de login o por correo de recuperación. */
    private Login buscarLoginPorCorreo(String email) {
        return loginRepository.findByEmailUsuario(email)
                .or(() -> loginRepository.findByEmailRecuperacion(email))
                .orElse(null);
    }

    String generarCodigo() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /** Genera y envía el código de recuperación al correo alternativo (o al de login). */
    public void solicitarRecuperacion(String email) {
        String emailNormalizado = normalizarEmail(email);
        Login login = buscarLoginPorCorreo(emailNormalizado);
        if (login == null) {
            throw new RuntimeException("No se encontró una cuenta con ese correo");
        }

        String codigo = generarCodigo();
        login.setCodigoRecuperacion(codigo);
        login.setCodigoRecuperacionExpira(LocalDateTime.now().plusMinutes(MINUTOS_CODIGO_RECUPERACION));
        loginRepository.save(login);

        String destinatario = login.getEmailRecuperacion() != null && !login.getEmailRecuperacion().isBlank()
                ? login.getEmailRecuperacion()
                : login.getEmailUsuario();

        String html = "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;"
                + "border:1px solid #e2e8f0;border-radius:12px;overflow:hidden\">"
                + "<div style=\"background:#4338ca;padding:20px;text-align:center\">"
                + "<h2 style=\"color:#ffffff;margin:0\">Kinetic Pulse</h2>"
                + "<p style=\"color:#dbeafe;margin:4px 0 0\">Recuperación de cuenta</p></div>"
                + "<div style=\"padding:24px\">"
                + "<p>Hola,</p>"
                + "<p>Recibimos una solicitud para recuperar tu cuenta. Usa el siguiente código:</p>"
                + "<p style=\"text-align:center;font-size:32px;font-weight:bold;letter-spacing:8px;color:#4338ca\">"
                + codigo + "</p>"
                + "<p style=\"color:#475569;font-size:13px\">El código expira en " + MINUTOS_CODIGO_RECUPERACION
                + " minutos. Si no solicitaste esta recuperación, ignora este correo.</p>"
                + "</div></div>";

        boolean enviado = emailService.enviarSeguro(destinatario,
                "Código de recuperación — Kinetic Pulse", html);
        if (!enviado) {
            login.setCodigoRecuperacion(null);
            login.setCodigoRecuperacionExpira(null);
            loginRepository.save(login);
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        }

        auditoriaService.registrar("RECUPERACION_SOLICITADA", "LOGIN",
                login.getUsuario().getIdUsuario(),
                "Código de recuperación enviado a " + destinatario,
                login.getUsuario().getIdUsuario(), ipActual());
    }

    /** Valida el código recibido y emite un token temporal para restablecer la contraseña. */
    public String verificarCodigo(String email, String codigo) {
        String emailNormalizado = normalizarEmail(email);
        Login login = buscarLoginPorCorreo(emailNormalizado);
        if (login == null || login.getCodigoRecuperacion() == null) {
            throw new RuntimeException("Código incorrecto o cuenta no registrada");
        }
        if (!login.getCodigoRecuperacion().equals(codigo)) {
            throw new RuntimeException("Código incorrecto");
        }
        if (login.getCodigoRecuperacionExpira() == null
                || login.getCodigoRecuperacionExpira().isBefore(LocalDateTime.now())) {
            login.setCodigoRecuperacion(null);
            login.setCodigoRecuperacionExpira(null);
            loginRepository.save(login);
            throw new RuntimeException("El código ha expirado. Solicita uno nuevo.");
        }

        login.setCodigoRecuperacion(null);
        login.setCodigoRecuperacionExpira(null);
        loginRepository.save(login);

        auditoriaService.registrar("CODIGO_VERIFICADO", "LOGIN",
                login.getUsuario().getIdUsuario(),
                "Código de recuperación validado", login.getUsuario().getIdUsuario(), ipActual());

        return jwtUtil.generarTokenReset(login.getEmailUsuario(), login.getUsuario().getIdUsuario());
    }

    /** Restablece la contraseña con el token temporal y desbloquea la cuenta. */
    public void restablecerContrasena(String token, String nuevaPassword) {
        if (token == null || token.isBlank() || !jwtUtil.esTokenValido(token)) {
            throw new RuntimeException("El token no es válido o ha expirado");
        }

        var claims = jwtUtil.extraerClaims(token);
        if (!"RESET".equals(claims.get("tipo", String.class))) {
            throw new RuntimeException("El token no es válido o ha expirado");
        }

        Login login = loginRepository.findByUsuario_IdUsuario(jwtUtil.extraerIdUsuario(token)).orElse(null);
        if (login == null
                || !login.getEmailUsuario().equalsIgnoreCase(jwtUtil.extraerEmail(token))) {
            throw new RuntimeException("El token no es válido o ha expirado");
        }

        login.setContrasenaUsuario(passwordEncoder.encode(nuevaPassword));
        login.setIntentosFallidos(0);
        login.setFechaBloqueo(null);
        login.setUltimoIntentoFallido(null);
        login.setCodigoRecuperacion(null);
        login.setCodigoRecuperacionExpira(null);
        login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO));
        loginRepository.save(login);

        auditoriaService.registrar("CONTRASENA_RESTABLECIDA", "LOGIN",
                login.getUsuario().getIdUsuario(),
                "Contraseña restablecida mediante código de recuperación",
                login.getUsuario().getIdUsuario(), ipActual());
    }

    private String construirNombreCompleto(Usuario u) {
        StringBuilder sb = new StringBuilder(u.getPrimerNombreUsuario());
        if (u.getSegundoNombreUsuario() != null && !u.getSegundoNombreUsuario().isBlank()) {
            sb.append(" ").append(u.getSegundoNombreUsuario());
        }
        sb.append(" ").append(u.getPrimerApellidoUsuario());
        if (u.getSegundoApellidoUsuario() != null && !u.getSegundoApellidoUsuario().isBlank()) {
            sb.append(" ").append(u.getSegundoApellidoUsuario());
        }
        return sb.toString();
    }

    private String normalizarTipoDocumento(String tipo) {
        String normalizado = TipoDocumentoUtils.normalizar(tipo);
        if (normalizado == null) {
            throw new RuntimeException("El tipo de documento no es válido");
        }
        return normalizado;
    }
}
package Eventos.eventos.config;

import Eventos.eventos.entity.Estado;
import Eventos.eventos.entity.Login;
import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.repository.EstadoRepository;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.RolRepository;
import Eventos.eventos.repository.UsuarioRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.util.RolNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LoginRepository loginRepository;
    @Autowired private EstadoRepository estadoRepository;
    @Autowired private EstadoService estadoService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        crearRolSiNoExiste(RolNames.SUPER_ADMIN, "Rol superior: es el único que puede restablecer registros eliminados");
        crearRolSiNoExiste(RolNames.ADMIN,       "Administrador del sistema con acceso total");
        crearRolSiNoExiste(RolNames.OPERADOR,    "Puede gestionar eventos, check-in e importaciones");
        crearRolSiNoExiste(RolNames.MONITOR,     "Acceso de solo lectura al dashboard y reportes");
        crearRolSiNoExiste(RolNames.INVITADO,    "Acceso público al catálogo de eventos");

        crearEstadoSiNoExiste("USUARIO", "Activo");
        crearEstadoSiNoExiste("USUARIO", "Inactivo");
        crearEstadoSiNoExiste("USUARIO", "Bloqueado");
        crearEstadoSiNoExiste("USUARIO", "Eliminado");

        crearEstadoSiNoExiste("LOGIN", "Activo");
        crearEstadoSiNoExiste("LOGIN", "Inactivo");
        crearEstadoSiNoExiste("LOGIN", "Bloqueado");
        crearEstadoSiNoExiste("LOGIN", "Eliminado");

        crearEstadoSiNoExiste("EVENTO", "Borrador");
        crearEstadoSiNoExiste("EVENTO", "Activo");
        crearEstadoSiNoExiste("EVENTO", "Finalizado");
        crearEstadoSiNoExiste("EVENTO", "Cancelado");

        crearEstadoSiNoExiste("REGISTRO_ASISTENCIA", "Registrado");
        crearEstadoSiNoExiste("REGISTRO_ASISTENCIA", "Asistio");
        crearEstadoSiNoExiste("REGISTRO_ASISTENCIA", "Eliminado");

        crearEstadoSiNoExiste("NOTIFICACION", "Programada");
        crearEstadoSiNoExiste("NOTIFICACION", "Enviada");
        crearEstadoSiNoExiste("NOTIFICACION", "Leida");

        crearEstadoSiNoExiste("CERTIFICADO", "Activo");
        crearEstadoSiNoExiste("CERTIFICADO", "Revocado");

        crearEstadoSiNoExiste("IMPORTACION", "Completado");
        crearEstadoSiNoExiste("IMPORTACION", "Completado Con Errores");

        crearAdminPorDefecto();
    }

    private void crearRolSiNoExiste(String nombre, String descripcion) {
        if (!rolRepository.existsByNombreRolIgnoreCase(nombre)) {
            Rol rol = new Rol();
            rol.setNombreRol(nombre);
            rol.setDescripcion(descripcion);
            rolRepository.save(rol);
            log.info("✅ Rol creado: {}", nombre);
        }
    }

    private void crearEstadoSiNoExiste(String tipo, String nombre) {
        boolean existe = estadoRepository
                .findByTipoEstadoIgnoreCaseAndNombreEstadoIgnoreCase(tipo, nombre)
                .isPresent();
        if (!existe) {
            Estado estado = new Estado();
            estado.setNombreEstado(nombre);
            estado.setTipoEstado(tipo);
            estado.setDescripcionEstado("Estado inicial del sistema");
            estadoRepository.save(estado);
            log.info("✅ Estado creado: {} ({})", nombre, tipo);
        }
    }

    private void crearAdminPorDefecto() {
        boolean existeAdmin = rolRepository.findByNombreRolIgnoreCase(RolNames.ADMIN)
                .map(usuarioRepository::existsByRol)
                .orElse(false);

        if (!existeAdmin) {
            Rol rolAdmin = rolRepository.findByNombreRolIgnoreCase(RolNames.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Rol " + RolNames.ADMIN + " no encontrado"));
            Rol rolSuper = rolRepository.findByNombreRolIgnoreCase(RolNames.SUPER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Rol " + RolNames.SUPER_ADMIN + " no encontrado"));

            Usuario admin = new Usuario();
            admin.setPrimerNombreUsuario("Juan");
            admin.setSegundoNombreUsuario("Manuel");
            admin.setPrimerApellidoUsuario("Villamil");
            admin.setSegundoApellidoUsuario("Vargas");
            admin.setTipoDocumentoUsuario("CC");
            admin.setNumeroDocumentoUsuario("1000521258");
            admin.setRoles(new java.util.HashSet<>(Set.of(rolSuper, rolAdmin)));
            admin.setEstado(estadoService.resolver(EstadoService.TIPO_USUARIO, "Activo"));
            admin = usuarioRepository.save(admin);

            Login login = new Login();
            login.setUsuario(admin);
            login.setEmailUsuario("admin@kineticpulse.com");
            login.setContrasenaUsuario(passwordEncoder.encode("Admin1234*"));
            login.setEstado(estadoService.resolver(EstadoService.TIPO_LOGIN, Login.ESTADO_ACTIVO));
            loginRepository.save(login);

            log.info("✅ Usuario admin creado:");
            log.info("   Email:      admin@kineticpulse.com");
            log.info("   Contraseña: Admin1234*");
            log.info("   Rol:        {} + {}", RolNames.SUPER_ADMIN, RolNames.ADMIN);
            log.info("   ⚠️  Cambia esta contraseña en producción.");
        }

        garantizarSuperAdminAlAdminPorDefecto();
    }

    /** Idempotente: asegura que el admin por defecto tenga el rol de Super Administrador. */
    private void garantizarSuperAdminAlAdminPorDefecto() {
        Rol rolSuper = rolRepository.findByNombreRolIgnoreCase(RolNames.SUPER_ADMIN).orElse(null);
        if (rolSuper == null) return;

        loginRepository.findByEmailUsuario("admin@kineticpulse.com").ifPresent(login -> {
            boolean yaTiene = login.getUsuario().getRoles().stream()
                    .anyMatch(r -> r.getNombreRol() != null
                            && r.getNombreRol().equalsIgnoreCase(RolNames.SUPER_ADMIN));
            if (!yaTiene) {
                login.getUsuario().getRoles().add(rolSuper);
                usuarioRepository.save(login.getUsuario());
                log.info("✅ Rol {} asignado a admin@kineticpulse.com", RolNames.SUPER_ADMIN);
            }
        });
    }
}

package Eventos.eventos.service;

import Eventos.eventos.entity.Estado;
import Eventos.eventos.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EstadoService {

    public static final String TIPO_USUARIO = "USUARIO";
    public static final String TIPO_LOGIN = "LOGIN";
    public static final String TIPO_EVENTO = "EVENTO";
    public static final String TIPO_REGISTRO = "REGISTRO_ASISTENCIA";
    public static final String TIPO_NOTIFICACION = "NOTIFICACION";
    public static final String TIPO_CERTIFICADO = "CERTIFICADO";
    public static final String TIPO_IMPORTACION = "IMPORTACION";

    private final Map<String, Estado> cache = new ConcurrentHashMap<>();

    @Autowired
    private EstadoRepository estadoRepository;

    @Transactional
    public Estado resolver(String tipoEstado, String nombreEstado) {
        if (nombreEstado == null || nombreEstado.isBlank()) {
            return null;
        }
        String clave = tipoEstado + ":" + capitalizar(nombreEstado);
        return cache.computeIfAbsent(clave, k -> estadoRepository
                .findByTipoEstadoIgnoreCaseAndNombreEstadoIgnoreCase(tipoEstado, capitalizar(nombreEstado))
                .orElseGet(() -> crearEstado(tipoEstado, nombreEstado)));
    }

    /**
     * Normaliza el nombre a "Mayúscula inicial por palabra, resto minúsculas".
     * Separa por guiones bajos y espacios. Ej: "COMPLETADO_CON_ERRORES" -> "Completado Con Errores".
     */
    public static String capitalizar(String nombre) {
        if (nombre == null) return null;
        String[] palabras = nombre.trim().toLowerCase().split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private Estado crearEstado(String tipoEstado, String nombreEstado) {
        Estado nuevo = new Estado();
        nuevo.setNombreEstado(capitalizar(nombreEstado));
        nuevo.setTipoEstado(tipoEstado);
        nuevo.setDescripcionEstado("Generado automáticamente");
        return estadoRepository.save(nuevo);
    }

    @Transactional(readOnly = true)
    public Estado existente(String tipoEstado, String nombreEstado) {
        if (nombreEstado == null || nombreEstado.isBlank()) {
            return null;
        }
        return estadoRepository
                .findByTipoEstadoIgnoreCaseAndNombreEstadoIgnoreCase(tipoEstado, nombreEstado)
                .orElse(null);
    }

    public String nombre(Estado estado) {
        return estado != null ? estado.getNombreEstado() : null;
    }
}

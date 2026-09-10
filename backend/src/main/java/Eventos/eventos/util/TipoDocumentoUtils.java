package Eventos.eventos.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public final class TipoDocumentoUtils {

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("cc", "CC");
        ALIASES.put("cedula", "CC");
        ALIASES.put("ceduladeciudadania", "CC");
        ALIASES.put("documentodeidentidad", "CC");
        ALIASES.put("ti", "TI");
        ALIASES.put("tarjetadeidentidad", "TI");
        ALIASES.put("ce", "CE");
        ALIASES.put("ceduladeextranjeria", "CE");
        ALIASES.put("extranjeria", "CE");
        ALIASES.put("pa", "PA");
        ALIASES.put("pasaporte", "PA");
        ALIASES.put("nit", "NIT");
        ALIASES.put("rc", "RC");
        ALIASES.put("registrocivil", "RC");
        ALIASES.put("registrocivildenacimiento", "RC");
    }

    private TipoDocumentoUtils() {
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String clave = limpiar(valor);
        return clave.isEmpty() ? null : ALIASES.get(clave);
    }

    private static String limpiar(String valor) {
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase().replaceAll("[^a-z]", "");
    }
}
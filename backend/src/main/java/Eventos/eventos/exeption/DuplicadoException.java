package Eventos.eventos.exeption;

/**
 * Conflicto de registro: un dato ya existe en la base de datos.
 * Incluye el nombre del campo para que el frontend marque el input culpable.
 */
public class DuplicadoException extends RuntimeException {

    private final String campo;

    public DuplicadoException(String message, String campo) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
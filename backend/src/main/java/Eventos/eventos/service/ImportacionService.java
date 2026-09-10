package Eventos.eventos.service;

import Eventos.eventos.dto.ImportacionDTO;
import org.springframework.web.multipart.MultipartFile;
import Eventos.eventos.exception.EventoFinalizadoException;
import java.util.List;

public interface ImportacionService {
    ImportacionDTO previsualizarCsv(MultipartFile archivo, Long idEvento);
    ImportacionDTO importarParticipantes(MultipartFile archivo, Long idEvento, Long idUsuario);
    List<ImportacionDTO> listarHistorial();
    ImportacionDTO obtenerPorId(Long id);
}

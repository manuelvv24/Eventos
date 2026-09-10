
package Eventos.eventos.service;

import Eventos.eventos.dto.CheckInDTO;
import Eventos.eventos.dto.CheckInMasivoDTO;
import Eventos.eventos.dto.RegistroBusquedaDTO;
import java.util.List;

public interface CheckInService {
    CheckInDTO realizarCheckIn(CheckInDTO dto);
    List<CheckInDTO> obtenerTodos();
    List<CheckInDTO> obtenerPorAsistencia(Long idRegistroAsistencia);

    // Búsqueda por número de identificación para check-in manual
    List<RegistroBusquedaDTO> buscarPorDocumento(String numeroDocumento, String tipoDocumento);
    
    // Nuevos métodos para check-in masivo
    CheckInMasivoDTO previsualizarCheckInMasivo(Long idEvento);
    CheckInMasivoDTO ejecutarCheckInMasivo(Long idEvento, Long idOperador, String ip);
    CheckInDTO anularCheckIn(Long idRegistroAsistencia, Long idOperador, String ip);
}
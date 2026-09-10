package Eventos.eventos.service.impl;

import Eventos.eventos.dto.ImportacionDTO;
import Eventos.eventos.dto.ImportacionDTO.ErrorFilaDTO;
import Eventos.eventos.dto.ImportacionDTO.ParticipanteImportDTO;
import Eventos.eventos.entity.Estado;
import Eventos.eventos.entity.Evento;
import Eventos.eventos.entity.Importacion;
import Eventos.eventos.entity.Rol;
import Eventos.eventos.entity.Usuario;
import Eventos.eventos.exception.EventoFinalizadoException;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.repository.ImportacionRepository;
import Eventos.eventos.repository.LoginRepository;
import Eventos.eventos.repository.ParticipanteRepository;
import Eventos.eventos.repository.RegistroAsistenciaRepository;
import Eventos.eventos.repository.RolRepository;
import Eventos.eventos.repository.UsuarioRepository;
import Eventos.eventos.service.EstadoService;
import Eventos.eventos.service.ImportacionService;
import Eventos.eventos.util.TipoDocumentoUtils;
import Eventos.eventos.util.RolNames;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ImportacionServiceImpl implements ImportacionService {

    private static final int COL_PRIMER_NOMBRE     = 0;
    private static final int COL_SEGUNDO_NOMBRE    = 1;
    private static final int COL_PRIMER_APELLIDO   = 2;
    private static final int COL_SEGUNDO_APELLIDO  = 3;
    private static final int COL_EMAIL             = 4;
    private static final int COL_TIPO_DOCUMENTO    = 5;
    private static final int COL_NUMERO_DOCUMENTO  = 6;
    private static final int COL_TELEFONO          = 7;
    private static final int COLUMNAS_MINIMAS      = 7;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");

    @Autowired private EventoRepository eventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LoginRepository loginRepository;
    @Autowired private ParticipanteRepository participanteRepository;
    @Autowired private RegistroAsistenciaRepository asistenciaRepository;
    @Autowired private ImportacionRepository importacionRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private EstadoService estadoService;
    @Autowired private ParticipanteImportWorker importWorker;

    @Override
    public ImportacionDTO previsualizarCsv(MultipartFile archivo, Long idEvento) {
        Evento evento = validarEventoImportable(idEvento);
        ResultadoAnalisis resultado = analizarArchivo(archivo, idEvento);

        validarAforo(evento, resultado.validas.size());

        ImportacionDTO dto = new ImportacionDTO();
        dto.setNombreArchivo(archivo.getOriginalFilename());
        dto.setIdEvento(idEvento);
        dto.setTotalFilas(resultado.totalFilas);
        dto.setFilasExitosas(resultado.validas.size());
        dto.setFilasError(resultado.errores.size());
        dto.setEstadoImportacion("PREVIEW");
        dto.setPreview(resultado.validas);
        dto.setErrores(resultado.errores);
        return dto;
    }

    @Override
    @Transactional
    public ImportacionDTO importarParticipantes(MultipartFile archivo, Long idEvento, Long idUsuario) {
        Evento evento = validarEventoImportable(idEvento);

        Rol rolInvitado = rolRepository.findByNombreRolIgnoreCase(RolNames.INVITADO)
                .orElseGet(() -> rolRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("No existen roles en la BD")));

        ResultadoAnalisis resultado = analizarArchivo(archivo, idEvento);

        validarAforo(evento, resultado.validas.size());

        int exitosas = 0;
        List<ErrorFilaDTO> erroresPersistencia = new ArrayList<>(resultado.errores);

        for (ParticipanteImportDTO fila : resultado.validas) {
            try {
                importWorker.procesarFila(fila, rolInvitado, evento);
                exitosas++;
            } catch (Exception e) {
                erroresPersistencia.add(new ErrorFilaDTO(
                        fila.getNumeroFila(),
                        fila.getEmail(),
                        "Error al persistir: " + e.getMessage()
                ));
            }
        }

        Importacion importacion = new Importacion();
        if (idUsuario != null) {
            importacion.setUsuario(usuarioRepository.findById(idUsuario).orElse(null));
        }
        importacion.setNombreArchivoImportacion(archivo.getOriginalFilename());
        importacion.setEvento(evento);
        importacion.setTotalFilaImportacion(resultado.totalFilas);
        importacion.setFilaExitosaImportacion(exitosas);
        importacion.setFilaErrorImportacion(erroresPersistencia.size());
        String nombreEstado = erroresPersistencia.isEmpty() ? "COMPLETADO" : "COMPLETADO_CON_ERRORES";
        Estado estadoImp = estadoService.resolver(EstadoService.TIPO_IMPORTACION, nombreEstado);
        if (estadoImp != null) {
            importacion.setEstado(estadoImp);
        }
        Importacion guardada = importacionRepository.save(importacion);

        ImportacionDTO dto = mapToDTO(guardada);
        dto.setErrores(erroresPersistencia);
        return dto;
    }

    @Override
    public List<ImportacionDTO> listarHistorial() {
        return importacionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ImportacionDTO obtenerPorId(Long id) {
        return mapToDTO(importacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Importación no encontrada con ID: " + id)));
    }

    private Evento validarEventoImportable(Long idEvento) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + idEvento));
        if (Boolean.TRUE.equals(evento.getEliminado())) {
            throw new RuntimeException(
                    "El evento '" + evento.getNombreEvento() + "' está en la papelera: no se pueden importar nuevos participantes.");
        }

        String estadoEvento = evento.getEstado() != null ? evento.getEstado().getNombreEstado() : null;
        String estadoUpper = String.valueOf(estadoEvento).toUpperCase();
        if ("FINALIZADO".equals(estadoUpper)) {
            throw new EventoFinalizadoException(
                    "El evento '" + evento.getNombreEvento() +
                    "' está FINALIZADO: no se pueden importar nuevos participantes.");
        }
        if ("CANCELADO".equals(estadoUpper)) {
            throw new EventoFinalizadoException(
                    "El evento '" + evento.getNombreEvento() +
                    "' está CANCELADO: no se pueden importar nuevos participantes.");
        }
        return evento;
    }

    private void validarAforo(Evento evento, int cantidadNueva) {
        if (cantidadNueva <= 0) return;

        long registrados = asistenciaRepository.findByEvento_IdEventos(evento.getIdEventos()).stream()
                .filter(r -> !r.estaEliminado())
                .count();
        long aforo       = evento.getAforoMaximoEvento() != null ? evento.getAforoMaximoEvento().longValue() : 0L;
        long disponibles = aforo - registrados;

        if (cantidadNueva > disponibles) {
            throw new RuntimeException(
                    "Aforo insuficiente: el evento '" + evento.getNombreEvento() +
                    "' tiene " + disponibles + " cupos disponibles y el archivo contiene " +
                    cantidadNueva + " participantes válidos. Reduce el archivo o aumenta el aforo.");
        }
    }

    // ── Dispatcher: CSV o Excel ───────────────────────────────────────────────
    private ResultadoAnalisis analizarArchivo(MultipartFile archivo, Long idEvento) {
        String nombre = (archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "").toLowerCase();
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return analizarExcel(archivo, idEvento);
        }
        return analizarCsv(archivo, idEvento);
    }

    // ── Lectura CSV ───────────────────────────────────────────────────────────
    private ResultadoAnalisis analizarCsv(MultipartFile archivo, Long idEvento) {
        List<ParticipanteImportDTO> validas = new ArrayList<>();
        List<ErrorFilaDTO> errores = new ArrayList<>();
        int totalFilas = 0;

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String[] cabecera = reader.readNext();
            if (cabecera == null) {
                errores.add(new ErrorFilaDTO(0, "", "El archivo CSV está vacío"));
                return new ResultadoAnalisis(0, validas, errores);
            }

            String[] fila;
            int numeroFila = 1;
            while ((fila = reader.readNext()) != null) {
                numeroFila++;
                totalFilas++;
                String contenido = String.join(",", fila);

                List<String> motivosError = validarFila(fila, numeroFila, idEvento);
                if (!motivosError.isEmpty()) {
                    errores.add(new ErrorFilaDTO(numeroFila, contenido,
                            String.join(" | ", motivosError)));
                } else {
                    validas.add(mapFila(fila, numeroFila));
                }
            }

        } catch (IOException | CsvValidationException e) {
            errores.add(new ErrorFilaDTO(0, "", "Error al leer el archivo: " + e.getMessage()));
        }

        return new ResultadoAnalisis(totalFilas, validas, errores);
    }

    // ── Lectura Excel (.xlsx / .xls) ──────────────────────────────────────────
    private ResultadoAnalisis analizarExcel(MultipartFile archivo, Long idEvento) {
        List<ParticipanteImportDTO> validas = new ArrayList<>();
        List<ErrorFilaDTO> errores = new ArrayList<>();
        int totalFilas = 0;

        String nombre = (archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "").toLowerCase();

        try (java.io.InputStream is = archivo.getInputStream()) {
            Workbook workbook = nombre.endsWith(".xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                errores.add(new ErrorFilaDTO(0, "", "El archivo Excel está vacío"));
                workbook.close();
                return new ResultadoAnalisis(0, validas, errores);
            }

            // Saltar la primera fila (cabecera)
            rowIterator.next();

            int numeroFila = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                numeroFila++;

                // Omitir filas completamente vacías
                if (isRowEmpty(row)) continue;

                totalFilas++;
                String[] fila = excelRowToStringArray(row);
                String contenido = String.join(",", fila);

                List<String> motivosError = validarFila(fila, numeroFila, idEvento);
                if (!motivosError.isEmpty()) {
                    errores.add(new ErrorFilaDTO(numeroFila, contenido, String.join(" | ", motivosError)));
                } else {
                    validas.add(mapFila(fila, numeroFila));
                }
            }

            workbook.close();
        } catch (IOException e) {
            errores.add(new ErrorFilaDTO(0, "", "Error al leer el archivo Excel: " + e.getMessage()));
        }

        return new ResultadoAnalisis(totalFilas, validas, errores);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String[] excelRowToStringArray(Row row) {
        int numCols = Math.max(row.getLastCellNum(), COLUMNAS_MINIMAS + 1);
        String[] datos = new String[numCols];
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < numCols; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            datos[i] = (cell != null) ? formatter.formatCellValue(cell).trim() : "";
        }
        return datos;
    }

    private List<String> validarFila(String[] fila, int numeroFila, Long idEvento) {
        List<String> errores = new ArrayList<>();

        if (fila.length < COLUMNAS_MINIMAS) {
            errores.add("Número de columnas insuficiente (mínimo " + COLUMNAS_MINIMAS + ")");
            return errores;
        }

        String primerNombre  = safe(fila, COL_PRIMER_NOMBRE);
        String primerApellido = safe(fila, COL_PRIMER_APELLIDO);
        String email         = safe(fila, COL_EMAIL);
        String tipoDoc       = safe(fila, COL_TIPO_DOCUMENTO);
        String numDoc        = safe(fila, COL_NUMERO_DOCUMENTO);

        if (primerNombre.isBlank())   errores.add("El primer nombre es obligatorio");
        if (primerApellido.isBlank()) errores.add("El primer apellido es obligatorio");
        if (email.isBlank())          errores.add("El email es obligatorio");
        if (!email.isBlank() && !EMAIL_REGEX.matcher(email).matches())
            errores.add("El email '" + email + "' no tiene un formato válido");
        if (tipoDoc.isBlank())        errores.add("El tipo de documento es obligatorio");
        else if (TipoDocumentoUtils.normalizar(tipoDoc) == null)
            errores.add("El tipo de documento '" + tipoDoc + "' no es reconocido");
        if (numDoc.isBlank())         errores.add("El número de documento es obligatorio");

        if (!numDoc.isBlank()) {
            String docLimpio = numDoc.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (docLimpio.length() < 5 || docLimpio.length() > 18)
                errores.add("El número de documento debe tener entre 5 y 18 caracteres");
        }
        String telefono = fila.length > COL_TELEFONO ? safe(fila, COL_TELEFONO) : "";
        if (!telefono.isBlank() && !telefono.replaceAll("[^0-9]", "").matches("\\d{7,15}"))
            errores.add("El teléfono debe tener entre 7 y 15 dígitos");
        if (!primerNombre.isBlank() && !primerNombre.trim().matches("[\\p{L} ]+"))
            errores.add("El primer nombre solo admite letras y espacios");
        else if (!primerNombre.isBlank() && (primerNombre.trim().length() < 2 || primerNombre.trim().length() > 30))
            errores.add("El primer nombre debe tener entre 2 y 30 caracteres");
        if (!primerApellido.isBlank() && !primerApellido.trim().matches("[\\p{L} ]+"))
            errores.add("El primer apellido solo admite letras y espacios");
        else if (!primerApellido.isBlank() && (primerApellido.trim().length() < 2 || primerApellido.trim().length() > 30))
            errores.add("El primer apellido debe tener entre 2 y 30 caracteres");

        if (!email.isBlank() && EMAIL_REGEX.matcher(email).matches()
                && loginRepository.existsByEmailUsuario(email)) {
            loginRepository.findByEmailUsuario(email).ifPresent(l -> {
                Usuario u = l.getUsuario();
                participanteRepository.findByUsuario_IdUsuario(u.getIdUsuario()).ifPresent(p -> {
                    boolean inscritoActivo = asistenciaRepository
                            .findByEvento_IdEventosAndParticipante_IdParticipante(
                                    idEvento, p.getIdParticipante())
                            .map(r -> !r.estaEliminado())
                            .orElse(false);
                    if (inscritoActivo) {
                        errores.add("El participante con email '" + email +
                                "' ya está inscrito en este evento");
                    }
                });
            });
        }

        return errores;
    }

    private ParticipanteImportDTO mapFila(String[] fila, int numeroFila) {
        ParticipanteImportDTO dto = new ParticipanteImportDTO();
        dto.setNumeroFila(numeroFila);
        dto.setPrimerNombre(limpiarNombre(safe(fila, COL_PRIMER_NOMBRE)));
        dto.setSegundoNombre(limpiarNombre(safe(fila, COL_SEGUNDO_NOMBRE)));
        dto.setPrimerApellido(limpiarNombre(safe(fila, COL_PRIMER_APELLIDO)));
        dto.setSegundoApellido(limpiarNombre(safe(fila, COL_SEGUNDO_APELLIDO)));
        dto.setEmail(safe(fila, COL_EMAIL).trim().toLowerCase());
        dto.setTipoDocumento(TipoDocumentoUtils.normalizar(safe(fila, COL_TIPO_DOCUMENTO)));
        dto.setNumeroDocumento(safe(fila, COL_NUMERO_DOCUMENTO).trim()
                .toUpperCase().replaceAll("[^A-Z0-9]", ""));
        String telefono = fila.length > COL_TELEFONO ? safe(fila, COL_TELEFONO) : "";
        dto.setTelefono(telefono.isBlank() ? "" : telefono.replaceAll("[^0-9]", ""));
        return dto;
    }

    private String limpiarNombre(String valor) {
        return valor.trim().replaceAll("\\s+", " ");
    }

    private String safe(String[] fila, int idx) {
        return (idx < fila.length && fila[idx] != null) ? fila[idx].trim() : "";
    }

    private ImportacionDTO mapToDTO(Importacion imp) {
        ImportacionDTO dto = new ImportacionDTO();
        dto.setIdImportacion(imp.getIdImportacion());
        dto.setIdUsuarios(imp.getUsuario() != null ? imp.getUsuario().getIdUsuario() : null);
        dto.setNombreArchivo(imp.getNombreArchivoImportacion());
        dto.setIdEvento(imp.getEvento() != null ? imp.getEvento().getIdEventos() : null);
        dto.setTotalFilas(imp.getTotalFilaImportacion());
        dto.setFilasExitosas(imp.getFilaExitosaImportacion());
        dto.setFilasError(imp.getFilaErrorImportacion());
        dto.setEstadoImportacion(imp.getEstado() != null ? imp.getEstado().getNombreEstado() : null);
        dto.setFechaImportacion(imp.getFechaImportacion());
        return dto;
    }

    private static class ResultadoAnalisis {
        final int totalFilas;
        final List<ParticipanteImportDTO> validas;
        final List<ErrorFilaDTO> errores;

        ResultadoAnalisis(int totalFilas, List<ParticipanteImportDTO> validas,
                          List<ErrorFilaDTO> errores) {
            this.totalFilas = totalFilas;
            this.validas    = validas;
            this.errores    = errores;
        }
    }
}
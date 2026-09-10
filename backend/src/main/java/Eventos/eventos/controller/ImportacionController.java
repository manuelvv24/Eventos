package Eventos.eventos.controller;

import Eventos.eventos.dto.ImportacionDTO;
import Eventos.eventos.service.ImportacionService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/importacion")
public class ImportacionController {

    @Autowired
    private ImportacionService importacionService;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previsualizarCsv(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("idEvento") Long idEvento) {
        try {
            validarArchivo(archivo);
            ImportacionDTO resultado = importacionService.previsualizarCsv(archivo, idEvento);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importarParticipantes(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("idEvento") Long idEvento,
            Authentication auth) {
        try {
            validarArchivo(archivo);
            Long idUsuario = null;
            if (auth != null && auth.getCredentials() instanceof Long id) {
                idUsuario = id;
            }
            ImportacionDTO resultado = importacionService.importarParticipantes(archivo, idEvento, idUsuario);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Descarga plantilla CSV */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantillaCsv() {
        String plantilla = "primerNombre,segundoNombre,primerApellido,segundoApellido,email,tipoDocumento,numeroDocumento,telefono\n" +
                "Juan,Carlos,Pérez,Gómez,juan.perez@example.com,CC,123456789,3001234567\n";
        byte[] bytes = plantilla.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "plantilla_importacion_participantes.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /** Descarga plantilla Excel (.xlsx) */
    @GetMapping("/plantilla-excel")
    public ResponseEntity<byte[]> descargarPlantillaExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Participantes");

            // Estilo encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columnas = {"primerNombre", "segundoNombre", "primerApellido", "segundoApellido",
                                 "email", "tipoDocumento", "numeroDocumento", "telefono"};

            Row header = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Fila de ejemplo
            Row ejemplo = sheet.createRow(1);
            Object[] valores = {"Juan", "Carlos", "Pérez", "Gómez",
                                "juan.perez@example.com", "CC", "123456789", "3001234567"};
            for (int i = 0; i < valores.length; i++) {
                ejemplo.createCell(i).setCellValue(valores[i].toString());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "plantilla_importacion_participantes.xlsx");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<List<ImportacionDTO>> listarHistorial() {
        return ResponseEntity.ok(importacionService.listarHistorial());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(importacionService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("El archivo no puede estar vacío");
        }
        String nombre = (archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "").toLowerCase();
        boolean valido = nombre.endsWith(".csv") || nombre.endsWith(".xlsx") || nombre.endsWith(".xls");
        if (!valido) {
            throw new RuntimeException("Formato de archivo no soportado. Solo se admiten archivos .csv, .xlsx o .xls");
        }
    }
}

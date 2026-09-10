package Eventos.eventos.controller;

import Eventos.eventos.dto.CertificadoDTO;
import Eventos.eventos.service.CertificadoService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private CertificadoService certificadoService;
    /** Health-check para Railway y load balancers. */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/qr/{codigo}")
    public ResponseEntity<?> generarImagenQr(@PathVariable String codigo) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(codigo, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("max-age=86400");
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo generar el QR"));
        }
    }
    private final Map<String, java.util.concurrent.ConcurrentLinkedQueue<Long>> requestCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    private boolean checkRateLimit(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        requestCounts.putIfAbsent(ip, new java.util.concurrent.ConcurrentLinkedQueue<>());
        var queue = requestCounts.get(ip);
        while (!queue.isEmpty() && now - queue.peek() > 60000) {
            queue.poll();
        }
        if (queue.size() >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        queue.add(now);
        return true;
    }

    @GetMapping("/verificar/{codigoVerificacion}")
    public ResponseEntity<?> verificarCertificado(@PathVariable String codigoVerificacion,
                                                  HttpServletRequest request) {
        if (!checkRateLimit(request)) {
            return ResponseEntity.status(429).body(Map.of("error", "Límite de solicitudes excedido. Intente más tarde."));
        }
        try {
            CertificadoDTO dto = certificadoService.verificarPorCodigo(codigoVerificacion);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                            "valido", false,
                            "mensaje", "Certificado no encontrado o código inválido.",
                            "codigo", codigoVerificacion
                    ));
        }
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificarCertificadoPost(@RequestBody Map<String, String> body,
                                                      HttpServletRequest request) {
        if (!checkRateLimit(request)) {
            return ResponseEntity.status(429).body(Map.of("error", "Límite de solicitudes excedido. Intente más tarde."));
        }
        String codigo = body.get("codigo");
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El campo 'codigo' es requerido"));
        }
        try {
            CertificadoDTO dto = certificadoService.verificarPorCodigo(codigo);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                            "valido", false,
                            "mensaje", "Certificado no encontrado o código inválido.",
                            "codigo", codigo
                    ));
        }
    }
}

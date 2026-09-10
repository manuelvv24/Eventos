package Eventos.eventos.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Sube una imagen desde un MultipartFile (formulario de evento).
     *
     * @param archivo archivo recibido desde el frontend
     * @param carpeta subcarpeta en Cloudinary, e.g. "eventos"
     * @return URL pública de la imagen
     */
    public String subirImagen(MultipartFile archivo, String carpeta) {
        validarArchivo(archivo);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder",          "gestion_eventos/" + carpeta,
                            "resource_type",   "image",
                            "use_filename",    true,
                            "unique_filename", true,
                            "quality",         "auto",
                            "fetch_format",    "auto"
                    )
            );
            String url = (String) resultado.get("secure_url");
            log.info("Imagen subida a Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Error al subir imagen a Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo subir la imagen: " + e.getMessage(), e);
        }
    }

    /**
     * Sube un PDF como bytes (generado en memoria).
     *
     * @param pdfBytes contenido del PDF
     * @param publicId identificador único (p. ej. el código de verificación del certificado)
     * @param carpeta  subcarpeta en Cloudinary, e.g. "certificados"
     * @return URL pública del PDF
     */
    public String subirPdf(byte[] pdfBytes, String publicId, String carpeta) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new RuntimeException("El contenido del PDF está vacío");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    pdfBytes,
                    ObjectUtils.asMap(
                            "folder",          "gestion_eventos/" + carpeta,
                            "public_id",       publicId,
                            "resource_type",   "raw",
                            "use_filename",    false,
                            "unique_filename", false,
                            "overwrite",       true
                    )
            );
            String url = (String) resultado.get("secure_url");
            log.info("PDF subido a Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Error al subir PDF a Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo subir el PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un recurso de Cloudinary por su public_id.
     * No lanza excepción si falla (operación de limpieza opcional).
     *
     * @param publicId     identificador del recurso en Cloudinary
     * @param resourceType "image" o "raw"
     */
    public void eliminar(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );
            log.info("Recurso eliminado de Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el recurso '{}' de Cloudinary: {}", publicId, e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("El archivo no puede estar vacío");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new RuntimeException("Solo se permiten archivos de imagen (jpg, png, webp, etc.)");
        }
        if (archivo.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("La imagen no puede superar 5 MB");
        }
    }
}

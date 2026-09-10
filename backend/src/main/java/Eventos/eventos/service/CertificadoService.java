package Eventos.eventos.service;

import Eventos.eventos.dto.CertificadoDTO;

import java.util.Map;

public interface CertificadoService {
    CertificadoDTO generarCertificado(Long idRegistroAsistencia);
    CertificadoDTO verificarPorCodigo(String codigoVerificacion);
    byte[] descargarPdf(Long idCertificado);
    byte[] descargarPdf(Long idCertificado, Long idPlantilla);
    CertificadoDTO obtenerPorId(Long idCertificado);
    CertificadoDTO revocarCertificado(Long idCertificado);

    /** Regenera el PDF de un certificado existente con otra plantilla (mantiene código de verificación). */
    CertificadoDTO regenerar(Long idRegistroAsistencia, Long idPlantilla);

    /** Regenera en lote los certificados emitidos de un evento. Devuelve {total, regenerados, errores}. */
    Map<String, Object> regenerarPorEvento(Long idEvento, Long idPlantilla);
}

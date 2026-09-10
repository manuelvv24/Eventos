package Eventos.eventos.service;

public interface PdfGeneratorService {
    String generarPdfCertificado(String nombreCompleto, String documento, String nombreEvento, String duracionEvento, String codigo);
}
package Eventos.eventos;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import Eventos.eventos.entity.Evento;
import Eventos.eventos.repository.EventoRepository;
import Eventos.eventos.service.PdfGeneratorService;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootTest
public class CertificadoRealDbTest {

    @BeforeAll
    static void setupEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        System.setProperty("spring.jpa.hibernate.ddl-auto", "none");
    }

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @Test
    void testGenerarCertificadoEventoReal() {
        List<Evento> eventos = eventoRepository.findAll();
        System.out.println("==================================================");
        System.out.println("TOTAL DE EVENTOS EN BD: " + eventos.size());
        
        if (eventos.isEmpty()) {
            System.out.println("No hay eventos en la BD.");
            return;
        }

        Evento eventoReal = eventos.get(0);
        System.out.println("-> Evento Encontrado: " + eventoReal.getNombreEvento() + " (" + eventoReal.getDuracionEvento() + " horas)");

        String codigoGenerado = "CERT-EVT-" + eventoReal.getIdEventos();
        
        // 👇 AQUÍ ESTABA EL ERROR: Se agregó String.valueOf() para convertir el Integer a String
        String duracionTexto = eventoReal.getDuracionEvento() != null ? 
                               String.valueOf(eventoReal.getDuracionEvento()) + " horas" : "No especificada";

        String rutaPdf = pdfGeneratorService.generarPdfCertificado(
                "CARLOS ANDRÉS MENDOZA",
                "1020304050",
                eventoReal.getNombreEvento(),
                duracionTexto,
                codigoGenerado
        );

        System.out.println("¡Certificado generado exitosamente para el evento real!");
        System.out.println("Ruta del archivo PDF: " + rutaPdf);
        System.out.println("==================================================");

        File pdfFile = new File(rutaPdf);
        assertTrue(pdfFile.exists(), "El PDF del certificado real debería existir");
    }
}
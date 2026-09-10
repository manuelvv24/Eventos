package Eventos.eventos.controller;

import Eventos.eventos.service.EmailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestMailController {

    private final EmailService emailService;

    public TestMailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/correo")
    public String probar(@RequestParam String para) {
        try {
            String html = "<h2>✅ El correo funciona</h2>"
                        + "<p>Esta es una prueba desde Spring Boot.</p>"
                        + "<p>Enviado a: <b>" + para + "</b></p>";
            emailService.enviarHtml(para, "Prueba de correo ✔", html);
            return "enviado a " + para;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
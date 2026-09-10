package Eventos.eventos;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@SpringBootTest
public class CertificadoGenerarRealTest {

    @BeforeAll
    static void setupEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        System.setProperty("spring.jpa.hibernate.ddl-auto", "none");
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void inspeccionarColumnasUsuarios() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("==================================================");
            System.out.println("COLUMNAS DE LA TABLA 'usuarios' EN LA BASE DE DATOS:");
            ResultSet rs = conn.getMetaData().getColumns(null, null, "usuarios", null);
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(" - " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
            }
            if (!found) {
                System.out.println("No se encontró la tabla 'usuarios' o está en minúsculas/mayúsculas.");
            }
            System.out.println("==================================================");
        }
    }
}

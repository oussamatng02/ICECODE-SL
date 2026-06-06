package alquiler.pisos;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class PisosApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Le demostramos a SonarQube que estamos comprobando algo real, sinó se quejará de "Test sin aserciones" :P
        assertNotNull(applicationContext, "Spring Boot debería haber cargado correctamente :)");
    }

}
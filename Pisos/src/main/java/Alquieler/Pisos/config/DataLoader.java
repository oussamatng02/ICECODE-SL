package Alquieler.Pisos.config;

import Alquieler.Pisos.entity.Inquilino;
import Alquieler.Pisos.entity.Propietario;
import Alquieler.Pisos.repository.UsuarioRepository;
import Alquieler.Pisos.service.InmuebleService;
import Alquieler.Pisos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Carga datos de demostración al arrancar en perfil de desarrollo.
 * Permite probar la aplicación sin necesidad de registro previo.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    private final UsuarioService usuarioService;
    private final InmuebleService inmuebleService;
    private final UsuarioRepository usuarioRepository;

    @Bean
    @Profile("!prod")   // No ejecutar en producción
    public CommandLineRunner cargarDatosDemostracion() {
        return args -> {
            if (usuarioRepository.count() > 0) return; // Ya hay datos

            log.info("Cargando datos de demostración...");

            // ─── Propietarios ───────────────────────────────────────
            Propietario carlos = usuarioService.registrarPropietario(
                "Carlos Martínez", "carlos@demo.com", "password123");
            Propietario lucia = usuarioService.registrarPropietario(
                "Lucía Pérez", "lucia@demo.com", "password123");

            // ─── Inquilinos ─────────────────────────────────────────
            Inquilino ana = usuarioService.registrarInquilino(
                "Ana García", "ana@demo.com", "password123");
            usuarioService.registrarInquilino(
                "Miguel Torres", "miguel@demo.com", "password123");

            // ─── Inmuebles de Carlos ─────────────────────────────────
            Inmueble atico = new Inmueble(
                "Ático con terraza en Malasaña",
                "Precioso ático con terraza privada en pleno corazón de Madrid. " +
                "Totalmente equipado, ideal para parejas o familias pequeñas. " +
                "A 5 minutos del metro Tribunal.",
                "Madrid, Malasaña",
                Inmueble.TipoInmueble.VIVIENDA_COMPLETA,
                85.0, carlos);
            atico.setCapacidadMaxima(4);
            atico.setTipoFlujo(Inmueble.TipoFlujoReserva.INMEDIATA);
            atico.setPoliticaCancelacion(Inmueble.PoliticaCancelacion.FLEXIBLE);
            atico.setWifi(true);
            atico.setAirConditioning(true);
            atico.setCocina(true);
            atico.setTelevisor(true);
            atico.setValoracion(4.8);
            inmuebleService.darDeAltaInmueble(atico);

            Inmueble estudio = new Inmueble(
                "Estudio moderno en Retiro",
                "Estudio reformado con todas las comodidades. " +
                "Vistas al parque del Retiro. Perfecto para trabajo remoto.",
                "Madrid, Retiro",
                Inmueble.TipoInmueble.VIVIENDA_COMPLETA,
                65.0, carlos);
            estudio.setCapacidadMaxima(2);
            estudio.setTipoFlujo(Inmueble.TipoFlujoReserva.SOLICITUD);
            estudio.setPoliticaCancelacion(Inmueble.PoliticaCancelacion.MODERADA);
            estudio.setWifi(true);
            estudio.setCocina(true);
            estudio.setValoracion(4.6);
            inmuebleService.darDeAltaInmueble(estudio);

            // ─── Inmuebles de Lucía ──────────────────────────────────
            Inmueble habitacion = new Inmueble(
                "Habitación privada en piso compartido",
                "Habitación luminosa en piso compartido con otras 2 personas. " +
                "Baño compartido. Zona muy bien comunicada.",
                "Madrid, Chueca",
                Inmueble.TipoInmueble.HABITACION_PRIVADA,
                40.0, lucia);
            habitacion.setCapacidadMaxima(1);
            habitacion.setTipoFlujo(Inmueble.TipoFlujoReserva.SOLICITUD);
            habitacion.setPoliticaCancelacion(Inmueble.PoliticaCancelacion.FLEXIBLE);
            habitacion.setWifi(true);
            habitacion.setValoracion(4.5);
            inmuebleService.darDeAltaInmueble(habitacion);

            Inmueble casaRural = new Inmueble(
                "Casa rural con jardín y BBQ",
                "Encantadora casa rural a 1 hora de Madrid. Jardín privado, " +
                "zona de barbacoa y piscina de temporada. Ideal para desconectar.",
                "Segovia, Sierra de Guadarrama",
                Inmueble.TipoInmueble.VIVIENDA_COMPLETA,
                120.0, lucia);
            casaRural.setCapacidadMaxima(8);
            casaRural.setTipoFlujo(Inmueble.TipoFlujoReserva.INMEDIATA);
            casaRural.setPoliticaCancelacion(Inmueble.PoliticaCancelacion.ESTRICTA);
            casaRural.setWifi(true);
            casaRural.setParking(true);
            casaRural.setCocina(true);
            casaRural.setTelevisor(true);
            casaRural.setValoracion(4.9);
            inmuebleService.darDeAltaInmueble(casaRural);

            Inmueble loft = new Inmueble(
                "Loft industrial en Lavapiés",
                "Loft de estilo industrial con techos altos y mucha luz natural. " +
                "Decoración única. En el barrio más multicultural de Madrid.",
                "Madrid, Lavapiés",
                Inmueble.TipoInmueble.VIVIENDA_COMPLETA,
                75.0, lucia);
            loft.setCapacidadMaxima(3);
            loft.setTipoFlujo(Inmueble.TipoFlujoReserva.INMEDIATA);
            loft.setPoliticaCancelacion(Inmueble.PoliticaCancelacion.MODERADA);
            loft.setWifi(true);
            loft.setAirConditioning(true);
            loft.setValoracion(4.7);
            inmuebleService.darDeAltaInmueble(loft);

            log.info("✅ Datos de demostración cargados.");
            log.info("   Propietarios: carlos@demo.com / lucia@demo.com (pass: password123)");
            log.info("   Inquilinos:   ana@demo.com / miguel@demo.com  (pass: password123)");
        };
    }
}

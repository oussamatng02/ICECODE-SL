package alquiler.pisos;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import alquiler.pisos.entity.Disponibilidad;
import alquiler.pisos.entity.Inmueble;
import alquiler.pisos.entity.Propietario;
import alquiler.pisos.repository.DisponibilidadRepository;
import alquiler.pisos.repository.InmuebleRepository;
import alquiler.pisos.service.InmuebleService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InmuebleServiceTest {

    @Mock
    private InmuebleRepository inmuebleRepository;

    @Mock
    private DisponibilidadRepository disponibilidadRepository;

    @InjectMocks
    private InmuebleService inmuebleService;

    // Variables globales para los tests
    private Inmueble inmuebleValido;
    private Propietario propietarioReal;
    private Propietario otroPropietario;

    @BeforeEach
    void setUp() {
        propietarioReal = new Propietario();
        propietarioReal.setId(1L);

        otroPropietario = new Propietario();
        otroPropietario.setId(2L);

        inmuebleValido = new Inmueble();
        inmuebleValido.setId(1L);
        inmuebleValido.setTitulo("Piso Madrid");
        inmuebleValido.setUbicacion("Madrid Centro");
        inmuebleValido.setPrecioPorNoche(50.0);
        inmuebleValido.setCapacidadMaxima(4);
        inmuebleValido.setPropietario(propietarioReal);
        inmuebleValido.setActivo(true);
    }

    // =========================================================
    // BLOQUE 1: darDeAltaInmueble (CP1, CP2, CP3)
    // =========================================================

    @Test
    void CP1_darDeAltaInmueble_OK() {
        // Arrange: Datos válidos
        when(inmuebleRepository.save(any(Inmueble.class))).thenReturn(inmuebleValido);

        // Act
        Inmueble resultado = inmuebleService.darDeAltaInmueble(inmuebleValido);

        // Assert
        assertNotNull(resultado);
        assertEquals(50.0, resultado.getPrecioPorNoche());
        verify(inmuebleRepository, times(1)).save(inmuebleValido);
    }

    @Test
    void CP2_darDeAltaInmueble_PrecioInvalido_LanzaException() {
        // Arrange: Precio negativo (Restricción de la entidad)
        inmuebleValido.setPrecioPorNoche(-10.0);
        
        // Simulamos que al intentar guardar, salta la validación de Spring (@Positive)
        when(inmuebleRepository.save(any(Inmueble.class))).thenThrow(ConstraintViolationException.class);

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            inmuebleService.darDeAltaInmueble(inmuebleValido);
        });
    }

    @Test
    void CP3_darDeAltaInmueble_CapacidadInvalida_LanzaException() {
        // Arrange: Capacidad mayor a 20 (Restricción de la entidad)
        inmuebleValido.setCapacidadMaxima(21);
        
        // Simulamos que al intentar guardar, salta la validación de Spring (@Max)
        when(inmuebleRepository.save(any(Inmueble.class))).thenThrow(ConstraintViolationException.class);

        // Act & Assert
        assertThrows(ConstraintViolationException.class, () -> {
            inmuebleService.darDeAltaInmueble(inmuebleValido);
        });
    }

    // =========================================================
    // BLOQUE 2: desactivarInmueble (CP4, CP5, CP6)
    // =========================================================

    @Test
    void CP4_desactivarInmueble_MismoPropietario_OK() {
        // Arrange
        when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleValido));

        // Act
        inmuebleService.desactivarInmueble(1L, propietarioReal);

        // Assert: Se cambia activo a false y se guarda
        assertFalse(inmuebleValido.isActivo());
        verify(inmuebleRepository, times(1)).save(inmuebleValido);
    }

    @Test
    void CP5_desactivarInmueble_NoExiste_LanzaException() {
        // Arrange
        when(inmuebleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inmuebleService.desactivarInmueble(999L, propietarioReal);
        });
        
        assertTrue(exception.getMessage().contains("Inmueble no encontrado"));
    }

    @Test
    void CP6_desactivarInmueble_DistintoPropietario_LanzaException() {
        // Arrange: El piso es de propietarioReal, pero intenta borrarlo otroPropietario
        when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleValido));

        // Act & Assert: Debe saltar la barrera de seguridad
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            inmuebleService.desactivarInmueble(1L, otroPropietario);
        });
        
        assertTrue(exception.getMessage().contains("No tienes permiso"));
        verify(inmuebleRepository, never()).save(any());
    }

    // =========================================================
    // BLOQUE 3: agregarDisponibilidad (CP7, CP8)
    // =========================================================

    @Test
    void CP7_agregarDisponibilidad_InmuebleExiste_OK() {
        // Arrange
        when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleValido));
        LocalDate inicio = LocalDate.now();
        LocalDate fin = LocalDate.now().plusDays(5);
        
        Disponibilidad dispGuardada = new Disponibilidad(inicio, fin, inmuebleValido);
        when(disponibilidadRepository.save(any(Disponibilidad.class))).thenReturn(dispGuardada);

        // Act
        Disponibilidad resultado = inmuebleService.agregarDisponibilidad(1L, inicio, fin);

        // Assert
        assertNotNull(resultado);
        verify(disponibilidadRepository, times(1)).save(any(Disponibilidad.class));
    }

    @Test
    void CP8_agregarDisponibilidad_InmuebleNoExiste_LanzaException() {
        // Arrange
        when(inmuebleRepository.findById(999L)).thenReturn(Optional.empty());
        LocalDate inicio = LocalDate.now();
        LocalDate fin = LocalDate.now().plusDays(5);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inmuebleService.agregarDisponibilidad(999L, inicio, fin);
        });
        
        verify(disponibilidadRepository, never()).save(any());
    }
}
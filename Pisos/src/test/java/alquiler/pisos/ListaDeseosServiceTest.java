package alquiler.pisos; 

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import alquiler.pisos.entity.Inmueble;
import alquiler.pisos.entity.Inquilino;
import alquiler.pisos.repository.InmuebleRepository;
import alquiler.pisos.repository.UsuarioRepository;
import alquiler.pisos.service.ListaDeseosService; 

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Tests para ListaDeseosService basados en la tabla de decisiones y MCD.
 * Condición A: El Inmueble (ID) existe en BD.
 * Condición B: El Inmueble está en la lista actual del inquilino.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListaDeseosService — CE, Decisión y MCD")
class ListaDeseosServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InmuebleRepository inmuebleRepository;

    @InjectMocks
    private ListaDeseosService listaDeseosService;

    private Inquilino inquilino;
    private Inmueble inmuebleExiste;

    @BeforeEach
    void setUp() {
        inquilino = new Inquilino("Ana", "ana@test.com", "pass");
        
        // Simular que la lista interna del inquilino existe (evitar NullPointerException)
        inquilino.setListaDeseos(new ArrayList<>());
        
        // Creamos el inmueble válido y le inyectamos el ID 1L
        inmuebleExiste = new Inmueble();
        setId(inmuebleExiste, 1L);
    }

    // =========================================================================
    // Tests para: agregar()
    // =========================================================================
    @Nested
    @DisplayName("Método: agregar()")
    class AgregarTests {

        @Test
        @DisplayName("CP1 · A=T, B=F → Inmueble existe y no está en lista → Añadido y guardado OK")
        void cp1_agregar_inmuebleExiste_noEnLista() {
            // Arrange
            when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleExiste));
            when(usuarioRepository.save(any(Inquilino.class))).thenReturn(inquilino);

            // Act
            listaDeseosService.agregar(inquilino, 1L);

            // Assert
            verify(usuarioRepository, times(1)).save(inquilino);
            assertThat(inquilino.getListaDeseos()).contains(inmuebleExiste);
        }

        @Test
        @DisplayName("CP2 · A=T, B=T → Inmueble existe y YA está en lista → Operación redundante, guardado OK")
        void cp2_agregar_inmuebleExiste_enLista() {
            // Arrange
            inquilino.getListaDeseos().add(inmuebleExiste); // Lo metemos antes (B=T)
            when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleExiste));
            when(usuarioRepository.save(any(Inquilino.class))).thenReturn(inquilino);

            // Act
            listaDeseosService.agregar(inquilino, 1L);

            // Assert
            verify(usuarioRepository, times(1)).save(inquilino);
            assertThat(inquilino.getListaDeseos()).hasSize(1); // No se duplica
        }

        @Test
        @DisplayName("CP3 · A=F → Inmueble no existe → IllegalArgumentException")
        void cp3_agregar_inmuebleNoExiste() {
            // Arrange
            when(inmuebleRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert (Lambda limpia sin variables complejas para SonarQube)
            Long idInvalido = 999L;
            assertThatThrownBy(() -> listaDeseosService.agregar(inquilino, idInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado: 999");
            
            verify(usuarioRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Tests para: eliminar()
    // =========================================================================
    @Nested
    @DisplayName("Método: eliminar()")
    class EliminarTests {

        @Test
        @DisplayName("CP4 · A=T, B=T → Inmueble existe y está en lista → Borrado y guardado OK")
        void cp4_eliminar_inmuebleExiste_enLista() {
            // Arrange
            inquilino.getListaDeseos().add(inmuebleExiste); // Lo metemos antes (B=T)
            when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleExiste));
            when(usuarioRepository.save(any(Inquilino.class))).thenReturn(inquilino);

            // Act
            listaDeseosService.eliminar(inquilino, 1L);

            // Assert
            verify(usuarioRepository, times(1)).save(inquilino);
            assertThat(inquilino.getListaDeseos()).doesNotContain(inmuebleExiste);
        }

        @Test
        @DisplayName("CP5 · A=T, B=F → Inmueble existe pero NO está en lista → Operación redundante, guardado OK")
        void cp5_eliminar_inmuebleExiste_noEnLista() {
            // Arrange (Lista vacía B=F)
            when(inmuebleRepository.findById(1L)).thenReturn(Optional.of(inmuebleExiste));
            when(usuarioRepository.save(any(Inquilino.class))).thenReturn(inquilino);

            // Act
            listaDeseosService.eliminar(inquilino, 1L);

            // Assert
            verify(usuarioRepository, times(1)).save(inquilino);
            assertThat(inquilino.getListaDeseos()).isEmpty(); // Sigue vacía
        }

        @Test
        @DisplayName("CP6 · A=F → Inmueble no existe → IllegalArgumentException")
        void cp6_eliminar_inmuebleNoExiste() {
            // Arrange
            when(inmuebleRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            Long idInvalido = 999L;
            assertThatThrownBy(() -> listaDeseosService.eliminar(inquilino, idInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado: 999");
            
            verify(usuarioRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Tests para: Consultas (obtenerLista y estaEnLista)
    // =========================================================================
    @Nested
    @DisplayName("Consultas: obtenerLista() y estaEnLista()")
    class ConsultasTests {

        @Test
        @DisplayName("CP7 · obtenerLista() → Devuelve List con inmuebles")
        void cp7_obtenerLista_devuelveLista() {
            // Arrange
            inquilino.getListaDeseos().add(inmuebleExiste);

            // Act
            List<Inmueble> lista = listaDeseosService.obtenerLista(inquilino);

            // Assert
            assertThat(lista).hasSize(1).contains(inmuebleExiste);
        }

        @Test
        @DisplayName("CP8 · estaEnLista() → Inmueble presente → Devuelve true")
        void cp8_estaEnLista_presente_true() {
            // Arrange
            inquilino.getListaDeseos().add(inmuebleExiste);

            // Act
            boolean resultado = listaDeseosService.estaEnLista(inquilino, 1L);

            // Assert
            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("CP9 · estaEnLista() → Inmueble no presente → Devuelve false")
        void cp9_estaEnLista_noPresente_false() {
            // Arrange (La lista no lo contiene)
            
            // Act
            boolean resultado = listaDeseosService.estaEnLista(inquilino, 999L);

            // Assert
            assertThat(resultado).isFalse();
        }
    }

    /**
     * Helper para inyectar ID en las entidades por reflexión (necesario para mockear IDs).
     */
    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException e) {
            try {
                var field = entity.getClass().getSuperclass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
            } catch (Exception ex) {
                throw new RuntimeException("Error asignando ID", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error asignando ID", e);
        }
    }
}
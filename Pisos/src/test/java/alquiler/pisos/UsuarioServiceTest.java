package alquiler.pisos;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.lenient;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import alquiler.pisos.entity.*;
import alquiler.pisos.repository.UsuarioRepository;
import alquiler.pisos.service.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de UsuarioService basados en el código real.
 *
 * Métodos analizados:
 *   registrarPropietario(nombre, email, contrasena)
 *   registrarInquilino(nombre, email, contrasena)
 *   actualizarPerfil(id, nombre)
 *   cambiarContrasena(id, nuevaContrasena)
 *   desactivarCuenta(id)
 *   buscarPorEmail(email)
 *   buscarPorId(id)
 *
 * Mensajes exactos del código:
 *   "El email ya está registrado: " + email  → validarEmailUnico()
 *   "Usuario no encontrado: " + id           → actualizarPerfil, cambiarContrasena, desactivarCuenta
 *
 * Condiciones:
 *   A: existsByEmail()=true   → IllegalArgumentException (registrar)
 *   B: findById().isEmpty()   → IllegalArgumentException (actualizar/cambiar/desactivar)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — CE + VL + Decisión + MCD")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder   passwordEncoder;

    @InjectMocks private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        // passwordEncoder.encode() siempre devuelve un hash simulado
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
    }

    // =========================================================================
    // CE — Clases de Equivalencia [registrarPropietario / registrarInquilino]
    // =========================================================================

    @Nested
    @DisplayName("CE registrar — Clases de Equivalencia")
    class CeRegistrar {

        /**
         * CP1 | CE válida: email nuevo (existsByEmail=false) → Propietario creado
         * Verifica: contrasena hasheada, nombre y email asignados
         */
        @Test
        @DisplayName("CP1 · CE válida · email nuevo → Propietario creado con contrasena hasheada")
        void cp1_registrarPropietario_emailNuevo_OK() {
            when(usuarioRepository.existsByEmail("carlos@test.com")).thenReturn(false);
            when(usuarioRepository.save(any(Propietario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            Propietario p = usuarioService.registrarPropietario(
                "Carlos", "carlos@test.com", "abc123");

            assertThat(p.getNombre()).isEqualTo("Carlos");
            assertThat(p.getEmail()).isEqualTo("carlos@test.com");
            assertThat(p.getContrasena()).isEqualTo("$2a$hashed");
            assertThat(p.isActivo()).isTrue();
            verify(usuarioRepository).save(any(Propietario.class));
            verify(passwordEncoder).encode("abc123");
        }

        /**
         * CP2 | A=T — existsByEmail()=true → validarEmailUnico lanza
         * Mensaje exacto: "El email ya está registrado: " + email
         */
        @Test
        @DisplayName("CP2 · CE inválida · A=T (email duplicado) → IllegalArgumentException: El email ya está registrado")
        void cp2_registrarPropietario_emailDuplicado_illegalArgument() {
            when(usuarioRepository.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() ->
                usuarioService.registrarPropietario("Carlos", "dup@test.com", "abc123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado: dup@test.com");

            verify(usuarioRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }

        /**
         * CP3 | CE válida: registrarInquilino con email nuevo
         * Verifica: tipo correcto (Inquilino), contrasena hasheada
         */
        @Test
        @DisplayName("CP3 · CE válida · registrarInquilino email nuevo → Inquilino creado")
        void cp3_registrarInquilino_emailNuevo_OK() {
            when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(false);
            when(usuarioRepository.save(any(Inquilino.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            Inquilino i = usuarioService.registrarInquilino(
                "Ana", "ana@test.com", "abc123");

            assertThat(i.getNombre()).isEqualTo("Ana");
            assertThat(i.getEmail()).isEqualTo("ana@test.com");
            assertThat(i.getContrasena()).isEqualTo("$2a$hashed");
            assertThat(i.isActivo()).isTrue();
        }

        /**
         * CP4 | A=T — registrarInquilino con email duplicado
         * Mismo mensaje exacto: "El email ya está registrado: " + email
         */
        @Test
        @DisplayName("CP4 · CE inválida · A=T registrarInquilino email duplicado → IllegalArgumentException")
        void cp4_registrarInquilino_emailDuplicado_illegalArgument() {
            when(usuarioRepository.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() ->
                usuarioService.registrarInquilino("Ana", "dup@test.com", "abc123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado: dup@test.com");

            verify(usuarioRepository, never()).save(any());
        }
    }

    // =========================================================================
    // VL — Valores Límite
    // =========================================================================

    @Nested
    @DisplayName("VL — Valores Límite")
    class ValoresLimite {

        /**
         * VL-CP1 | nombre = 2 chars — mínimo válido (@Size(min=2))
         */
        @Test
        @DisplayName("VL-CP1 · nombre='AB' (2 chars, mínimo válido) → Propietario creado")
        void vlCp1_nombreLongitudMinima_OK() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any(Propietario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            Propietario p = usuarioService.registrarPropietario(
                "AB", "vl@test.com", "abc123");

            assertThat(p.getNombre()).isEqualTo("AB").hasSize(2);
        }

        /**
         * VL-CP2 | nombre = 100 chars — máximo válido (@Size(max=100))
         */
        @Test
        @DisplayName("VL-CP2 · nombre=100 chars (máximo válido) → Propietario creado")
        void vlCp2_nombreLongitudMaxima_OK() {
            String nombre100 = "A".repeat(100);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any(Propietario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            Propietario p = usuarioService.registrarPropietario(
                nombre100, "vl2@test.com", "abc123");

            assertThat(p.getNombre()).hasSize(100);
        }

        /**
         * VL-CP3 | passwordEncoder.encode() llamado exactamente una vez
         * Verifica que la contraseña se hashea siempre, nunca se guarda en plano
         */
        @Test
        @DisplayName("VL-CP3 · contrasena siempre hasheada → encode() llamado 1 vez")
        void vlCp3_contrasenaSiempreHasheada() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.registrarPropietario("Carlos", "c@test.com", "miPassword");

            verify(passwordEncoder, times(1)).encode("miPassword");
        }

        /**
         * VL-CP4 | cambiarContrasena también hashea la nueva contraseña
         */
        @Test
        @DisplayName("VL-CP4 · cambiarContrasena → encode() llamado con la nueva contraseña")
        void vlCp4_cambiarContrasena_hashea() {
            Propietario p = new Propietario("Carlos", "c@test.com", "$2a$old");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.cambiarContrasena(1L, "nuevaPass");

            verify(passwordEncoder).encode("nuevaPass");
            assertThat(p.getContrasena()).isEqualTo("$2a$hashed");
        }

        /**
         * VL-CP5 | desactivarCuenta → activo cambia de true a false
         * Verifica el límite: activo=true → activo=false
         */
        @Test
        @DisplayName("VL-CP5 · desactivarCuenta → usuario.activo cambia true→false")
        void vlCp5_desactivarCuenta_activoCambia() {
            Propietario p = new Propietario("Carlos", "c@test.com", "hash");
            assertThat(p.isActivo()).isTrue(); // valor inicial

            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.desactivarCuenta(1L);

            assertThat(p.isActivo()).isFalse();
        }
    }

    // =========================================================================
    // DEC — Tabla de Decisión
    // =========================================================================

    @Nested
    @DisplayName("DEC — Tabla de Decisión")
    class TablaDecision {

        // ── registrarPropietario ──────────────────────────────────────────────

        /** DEC-CP1 | A=F → Dec2=T → Propietario creado */
        @Test
        @DisplayName("DEC-CP1 · A=F (email nuevo) → Propietario creado OK")
        void decCp1_aFalse_propietarioCreado() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any(Propietario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            assertThat(usuarioService.registrarPropietario("C","c@t.com","p"))
                .isInstanceOf(Propietario.class);
        }

        /** DEC-CP2 | A=T → Dec1=T → IllegalArgumentException */
        @Test
        @DisplayName("DEC-CP2 · A=T (email duplicado) → Dec1=T → IllegalArgumentException")
        void decCp2_aTrue_illegalArgument() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(true);

            assertThatThrownBy(() ->
                usuarioService.registrarPropietario("C","dup@t.com","p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado");
        }

        // ── registrarInquilino ────────────────────────────────────────────────

        /** DEC-CP3 | A=F → Dec2=T → Inquilino creado */
        @Test
        @DisplayName("DEC-CP3 · A=F registrarInquilino → Inquilino creado OK")
        void decCp3_aFalse_inquilinoCreado() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any(Inquilino.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            assertThat(usuarioService.registrarInquilino("A","a@t.com","p"))
                .isInstanceOf(Inquilino.class);
        }

        /** DEC-CP4 | A=T → Dec1=T → IllegalArgumentException */
        @Test
        @DisplayName("DEC-CP4 · A=T registrarInquilino → Dec1=T → IllegalArgumentException")
        void decCp4_aTrue_inquilino_illegalArgument() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(true);

            assertThatThrownBy(() ->
                usuarioService.registrarInquilino("A","dup@t.com","p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado");
        }

        // ── actualizarPerfil ──────────────────────────────────────────────────

        /** DEC-CP5 | B=F → Dec4=T → nombre actualizado */
        @Test
        @DisplayName("DEC-CP5 · B=F (id existe) actualizarPerfil → nombre actualizado")
        void decCp5_bFalse_perfilActualizado() {
            Propietario p = new Propietario("Carlos","c@t.com","hash");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Usuario u = usuarioService.actualizarPerfil(1L, "NuevoNombre");

            assertThat(u.getNombre()).isEqualTo("NuevoNombre");
        }

        /** DEC-CP6 | B=T → Dec3=T → IllegalArgumentException */
        @Test
        @DisplayName("DEC-CP6 · B=T (id no existe) actualizarPerfil → Dec3=T → IllegalArgumentException")
        void decCp6_bTrue_perfilNoExiste_illegalArgument() {
            when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                usuarioService.actualizarPerfil(99L, "Nombre"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado: 99");
        }

        // ── cambiarContrasena ─────────────────────────────────────────────────

        /** DEC-CP7 | B=F → Dec4=T → contraseña cambiada */
        @Test
        @DisplayName("DEC-CP7 · B=F (id existe) cambiarContrasena → contrasena actualizada")
        void decCp7_bFalse_contrasenaCambiada() {
            Propietario p = new Propietario("Carlos","c@t.com","$2a$old");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.cambiarContrasena(1L, "nueva123");

            assertThat(p.getContrasena()).isEqualTo("$2a$hashed");
            verify(usuarioRepository).save(p);
        }

        /** DEC-CP8 | B=T → Dec3=T → IllegalArgumentException */
        @Test
        @DisplayName("DEC-CP8 · B=T (id no existe) cambiarContrasena → Dec3=T → IllegalArgumentException")
        void decCp8_bTrue_cambiarContrasena_illegalArgument() {
            when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                usuarioService.cambiarContrasena(99L, "nueva"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado: 99");
        }

        // ── desactivarCuenta ──────────────────────────────────────────────────

        /** DEC-CP9 | B=F → Dec4=T → activo=false */
        @Test
        @DisplayName("DEC-CP9 · B=F (id existe) desactivarCuenta → usuario.activo=false")
        void decCp9_bFalse_cuentaDesactivada() {
            Propietario p = new Propietario("Carlos","c@t.com","hash");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.desactivarCuenta(1L);

            assertThat(p.isActivo()).isFalse();
            verify(usuarioRepository).save(p);
        }

        /** DEC-CP10 | B=T → Dec3=T → IllegalArgumentException */
        @Test
        @DisplayName("DEC-CP10 · B=T (id no existe) desactivarCuenta → Dec3=T → IllegalArgumentException")
        void decCp10_bTrue_desactivar_illegalArgument() {
            when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                usuarioService.desactivarCuenta(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado: 99");
        }
    }

    // =========================================================================
    // MCD — Condición Dominante Modificada (16 CPs)
    // =========================================================================

    @Nested
    @DisplayName("MCD — Condición Dominante Modificada")
    class CondicionDominante {

        /** MCD-CP1 | Caso base registrar: A=F → Propietario creado */
        @Test
        @DisplayName("MCD-CP1 · caso base registrarPropietario (A=F) → Propietario creado")
        void mcdCp1_casoBaseRegistrarPropietario() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any(Propietario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            assertThat(usuarioService.registrarPropietario("C","c@t.com","p"))
                .isNotNull();
        }

        /** MCD-CP2 | A=T (solo) domina → IllegalArgumentException */
        @Test
        @DisplayName("MCD-CP2 · A=T solo → A domina → IllegalArgumentException")
        void mcdCp2_aDomina_registrarPropietario() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(true);

            assertThatThrownBy(() ->
                usuarioService.registrarPropietario("C","dup@t.com","p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado");
        }

        /** MCD-CP3 | Caso base registrarInquilino: A=F → Inquilino creado */
        @Test
        @DisplayName("MCD-CP3 · caso base registrarInquilino (A=F) → Inquilino creado")
        void mcdCp3_casoBaseRegistrarInquilino() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.save(any(Inquilino.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            assertThat(usuarioService.registrarInquilino("A","a@t.com","p"))
                .isNotNull();
        }

        /** MCD-CP4 | A=T registrarInquilino → A domina */
        @Test
        @DisplayName("MCD-CP4 · A=T registrarInquilino → A domina → IllegalArgumentException")
        void mcdCp4_aDomina_registrarInquilino() {
            when(usuarioRepository.existsByEmail(any())).thenReturn(true);

            assertThatThrownBy(() ->
                usuarioService.registrarInquilino("A","dup@t.com","p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El email ya está registrado");
        }

        /** MCD-CP5 | Caso base actualizarPerfil: B=F → nombre actualizado */
        @Test
        @DisplayName("MCD-CP5 · caso base actualizarPerfil (B=F) → nombre actualizado")
        void mcdCp5_casoBaseActualizar() {
            Propietario p = new Propietario("Viejo","c@t.com","hash");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(usuarioService.actualizarPerfil(1L,"Nuevo").getNombre())
                .isEqualTo("Nuevo");
        }

        /** MCD-CP6 | B=T (solo) actualizarPerfil → B domina */
        @Test
        @DisplayName("MCD-CP6 · B=T solo actualizarPerfil → B domina → IllegalArgumentException")
        void mcdCp6_bDomina_actualizarPerfil() {
            when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                usuarioService.actualizarPerfil(99L,"Nombre"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado: 99");
        }

        /** MCD-CP7 | Caso base cambiarContrasena: B=F → contraseña cambiada */
        @Test
        @DisplayName("MCD-CP7 · caso base cambiarContrasena (B=F) → contrasena actualizada")
        void mcdCp7_casoBaseCambiarContrasena() {
            Propietario p = new Propietario("C","c@t.com","$2a$old");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.cambiarContrasena(1L,"nueva");

            assertThat(p.getContrasena()).isEqualTo("$2a$hashed");
        }

        /** MCD-CP8 | B=T cambiarContrasena → B domina */
        @Test
        @DisplayName("MCD-CP8 · B=T cambiarContrasena → B domina → IllegalArgumentException")
        void mcdCp8_bDomina_cambiarContrasena() {
            when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                usuarioService.cambiarContrasena(99L,"nueva"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado: 99");
        }

        /** MCD-CP9 | Caso base desactivarCuenta: B=F → activo=false */
        @Test
        @DisplayName("MCD-CP9 · caso base desactivarCuenta (B=F) → activo=false")
        void mcdCp9_casoBaseDesactivar() {
            Propietario p = new Propietario("C","c@t.com","hash");
            when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.desactivarCuenta(1L);

            assertThat(p.isActivo()).isFalse();
        }

        /** MCD-CP10 | B=T desactivarCuenta → B domina */
        @Test
        @DisplayName("MCD-CP10 · B=T desactivarCuenta → B domina → IllegalArgumentException")
        void mcdCp10_bDomina_desactivar() {
            when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                usuarioService.desactivarCuenta(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado: 99");
        }

        /**
         * MCD-CP11/CP12 | A=F y A=T en registrar — verificación simétrica
         * @ParameterizedTest: A=F → OK, A=T → error (Sonar: sustituye duplicados)
         */
        @ParameterizedTest(name = "existsByEmail={0} → {1}")
        @org.junit.jupiter.params.provider.CsvSource({
            "false, OK",
            "true,  ERROR"
        })
        @DisplayName("MCD-CP11/CP12 · A=F→Propietario OK / A=T→IllegalArgumentException")
        void mcdCp11Cp12_registrar_aFalseYTrue(boolean existe, String resultado) {
            when(usuarioRepository.existsByEmail(any())).thenReturn(existe);
            if (!existe) {
                when(usuarioRepository.save(any(Propietario.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
                assertThatNoException().isThrownBy(() ->
                    usuarioService.registrarPropietario("C","c@t.com","p"));
            } else {
                assertThatThrownBy(() ->
                    usuarioService.registrarPropietario("C","dup@t.com","p"))
                    .isInstanceOf(IllegalArgumentException.class);
            }
        }

        /**
         * MCD-CP13/CP14 | B=F y B=T en actualizarPerfil — verificación simétrica
         * @ParameterizedTest: sustituye los pares simétricos duplicados (Sonar)
         */
        @ParameterizedTest(name = "findById vacío={0} → {1}")
        @org.junit.jupiter.params.provider.CsvSource({
            "false, OK",
            "true,  ERROR"
        })
        @DisplayName("MCD-CP13/CP14 · B=F→nombre actualizado / B=T→IllegalArgumentException")
        void mcdCp13Cp14_actualizarPerfil_bFalseYTrue(boolean vacio, String resultado) {
            if (!vacio) {
                Propietario p = new Propietario("Viejo","c@t.com","hash");
                when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
                when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                assertThat(usuarioService.actualizarPerfil(1L,"Nuevo").getNombre())
                    .isEqualTo("Nuevo");
            } else {
                when(usuarioRepository.findById(any())).thenReturn(Optional.empty());
                assertThatThrownBy(() ->
                    usuarioService.actualizarPerfil(99L,"N"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado");
            }
        }

        /**
         * MCD-CP15/CP16 | B=F y B=T en desactivarCuenta — verificación simétrica
         */
        @ParameterizedTest(name = "findById vacío={0}")
        @org.junit.jupiter.params.provider.CsvSource({
            "false",
            "true"
        })
        @DisplayName("MCD-CP15/CP16 · B=F→activo=false / B=T→IllegalArgumentException")
        void mcdCp15Cp16_desactivar_bFalseYTrue(boolean vacio) {
            if (!vacio) {
                Propietario p = new Propietario("C","c@t.com","hash");
                when(usuarioRepository.findById(any())).thenReturn(Optional.of(p));
                when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                usuarioService.desactivarCuenta(1L);
                assertThat(p.isActivo()).isFalse();
            } else {
                when(usuarioRepository.findById(any())).thenReturn(Optional.empty());
                assertThatThrownBy(() -> usuarioService.desactivarCuenta(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado");
            }
        }
    }

    // =========================================================================
    // Métodos de consulta: buscarPorEmail / buscarPorId
    // =========================================================================

    @Nested
    @DisplayName("Consultas — buscarPorEmail / buscarPorId")
    class Consultas {

        @Test
        @DisplayName("buscarPorEmail · email existente → Optional.of(usuario)")
        void buscarPorEmail_existe_presente() {
            Propietario p = new Propietario("Carlos","c@t.com","hash");
            when(usuarioRepository.findByEmail("c@t.com")).thenReturn(Optional.of(p));

            assertThat(usuarioService.buscarPorEmail("c@t.com")).isPresent();
            verify(usuarioRepository).findByEmail("c@t.com");
        }

        @Test
        @DisplayName("buscarPorEmail · email no existente → Optional.empty()")
        void buscarPorEmail_noExiste_vacio() {
            when(usuarioRepository.findByEmail("no@t.com")).thenReturn(Optional.empty());

            assertThat(usuarioService.buscarPorEmail("no@t.com")).isEmpty();
        }

        @Test
        @DisplayName("buscarPorId · id existente → Optional.of(usuario)")
        void buscarPorId_existe_presente() {
            Propietario p = new Propietario("Carlos","c@t.com","hash");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThat(usuarioService.buscarPorId(1L)).isPresent();
        }

        @Test
        @DisplayName("buscarPorId · id no existente → Optional.empty()")
        void buscarPorId_noExiste_vacio() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(usuarioService.buscarPorId(99L)).isEmpty();
        }
    }
}

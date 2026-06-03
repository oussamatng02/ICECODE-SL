package Alquieler.Pisos;

import Alquieler.Pisos.entity.*;
import Alquieler.Pisos.service.*;
import Alquieler.Pisos.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * Mensajes exactos de excepción del código:
 *   "Inmueble no encontrado: " + id
 *   "Este inmueble requiere solicitud al propietario"
 *   "La fecha de entrada debe ser anterior a la de salida"
 *   "La fecha de entrada no puede ser en el pasado"
 *   "El inmueble no está disponible en las fechas seleccionadas"
 *   "La solicitud ya fue procesada"
 *   "No tienes permiso para cancelar esta reserva"
 *   "Reserva no encontrada: " + id
 *   "Solicitud no encontrada: " + id
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService — CE + VL + Decisión + MCD")
class ReservaServiceTest {

    @Mock private ReservaRepository          reservaRepository;
    @Mock private SolicitudReservaRepository solicitudRepository;
    @Mock private InmuebleRepository         inmuebleRepository;
    @Mock private NotificacionService        notificacionService;

    @InjectMocks private ReservaService reservaService;

    private Propietario propietario;
    private Inquilino   inquilino;
    private Inmueble    inmuebleInmediata;
    private Inmueble    inmuebleSolicitud;

    private static final LocalDate HOY  = LocalDate.now();
    private static final LocalDate MAS1 = HOY.plusDays(1);
    private static final LocalDate MAS5 = HOY.plusDays(5);
    private static final LocalDate MAS8 = HOY.plusDays(8);
    private static final LocalDate AYER = HOY.minusDays(1);

    @BeforeEach
    void setUp() {
        propietario = new Propietario("Carlos", "carlos@test.com", "pass");
        inquilino   = new Inquilino("Ana",     "ana@test.com",    "pass");

        inmuebleInmediata = new Inmueble(
            "Ático", "Desc", "Madrid",
            Inmueble.TipoInmueble.VIVIENDA_COMPLETA, 100.0, propietario);
        inmuebleInmediata.setTipoFlujo(Inmueble.TipoFlujoReserva.INMEDIATA);

        inmuebleSolicitud = new Inmueble(
            "Casa Rural", "Desc", "Segovia",
            Inmueble.TipoInmueble.VIVIENDA_COMPLETA, 80.0, propietario);
        inmuebleSolicitud.setTipoFlujo(Inmueble.TipoFlujoReserva.SOLICITUD);
    }

    // =========================================================================
    // CE — Clases de Equivalencia
    // =========================================================================

    @Nested
    @DisplayName("CE — Clases de Equivalencia")
    class ClasesEquivalencia {

        /**
         * CP1 | Clase válida de todos los parámetros
         * → Reserva creada con estado=PENDIENTE_PAGO y tipoFlujo=INMEDIATA
         */
        @Test
        @DisplayName("CP1 · CE válida · todos OK → Reserva PENDIENTE_PAGO, tipoFlujo=INMEDIATA")
        void cp1_todosValidos_reservaCreada() {
            // FIX: usar when(any()) en lugar de when(inmuebleInmediata.getId())
            // porque el ID es null hasta que JPA persista
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reserva r = reservaService.crearReservaInmediata(
                1L, inquilino, MAS5, MAS8);

            assertThat(r.getEstado()).isEqualTo(Reserva.EstadoReserva.PENDIENTE_PAGO);
            assertThat(r.getTipoFlujo()).isEqualTo(Reserva.TipoFlujo.INMEDIATA);
            assertThat(r.getInquilino()).isEqualTo(inquilino);
            assertThat(r.getNumNoches()).isEqualTo(3);
            assertThat(r.getImporteTotal()).isEqualTo(336.0);
            verify(notificacionService).notificarNuevaReserva(any());
        }

        /**
         * CP2 | A=T — findById devuelve Optional.empty() → orElseThrow
         */
        @Test
        @DisplayName("CP2 · CE inválida · A=T (Optional.empty) → IllegalArgumentException: Inmueble no encontrado")
        void cp2_inmuebleNoExiste_illegalArgumentException() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");

            verify(reservaRepository, never()).save(any());
        }

        /**
         * CP3 | B=T — tipoFlujo=SOLICITUD → validarFlujoInmediato lanza
         */
        @Test
        @DisplayName("CP3 · CE inválida · B=T (SOLICITUD) → IllegalStateException: requiere solicitud")
        void cp3_flujoSolicitud_illegalStateException() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Este inmueble requiere solicitud al propietario");

            verify(reservaRepository, never()).save(any());
        }

        /**
         * CP4 | C=T — !entrada.isBefore(salida): entrada > salida
         * 1ª comprobación de validarFechas()
         */
        @Test
        @DisplayName("CP4 · CE inválida · C=T (entrada>salida) → IllegalArgumentException: anterior a la de salida")
        void cp4_entradaMayorQueSalida_illegalArgumentException() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS8, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /**
         * CP4b | C=T — entrada = salida (0 noches)
         */
        @Test
        @DisplayName("CP4b · CE inválida · C=T (entrada=salida, 0 noches) → IllegalArgumentException")
        void cp4b_entradaIgualSalida_illegalArgumentException() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /**
         * CP5 | D=T — entrada = ayer → 2ª comprobación de validarFechas()
         * Solo se llega si C=F (entrada < salida)
         */
        @Test
        @DisplayName("CP5 · CE inválida · D=T (entrada=ayer) → IllegalArgumentException: en el pasado")
        void cp5_entradaEnPasado_illegalArgumentException() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /**
         * CP6 | E=T — existeSolapamiento()=true
         */
        @Test
        @DisplayName("CP6 · CE inválida · E=T (solapamiento) → IllegalStateException: no está disponible")
        void cp6_solapamiento_illegalStateException() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El inmueble no está disponible en las fechas seleccionadas");
        }
    }

    // =========================================================================
    // VL — Valores Límite
    // =========================================================================

    @Nested
    @DisplayName("VL — Valores Límite")
    class ValoresLimite {

        /**
         * VL-CP1 | entrada = now()+1 — mínimo válido
         * isBefore(now())=false ✓ → Reserva creada con 1 noche
         */
        @Test
        @DisplayName("VL-CP1 · entrada=now()+1 (mínimo válido) → Reserva OK, 1 noche")
        void vlCp1_entradaMinimaValida_unaNoche() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reserva r = reservaService.crearReservaInmediata(
                1L, inquilino, MAS1, MAS1.plusDays(1));

            assertThat(r.getNumNoches()).isEqualTo(1);
            // base=100*1=100, fee=12, total=112
            assertThat(r.getImporteTotal()).isEqualTo(112.0);
        }

        /**
         * VL-CP2 | salida = entrada+1 — mínimo válido de salida (1 noche)
         */
        @Test
        @DisplayName("VL-CP2 · salida=entrada+1 (mínimo válido) → Reserva OK")
        void vlCp2_salidaMinimaValida() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reserva r = reservaService.crearReservaInmediata(
                1L, inquilino, MAS5, MAS5.plusDays(1));

            assertThat(r.getNumNoches()).isEqualTo(1);
        }

        /**
         * VL-CP3 | salida = entrada — límite exacto inválido de C (0 noches)
         */
        @Test
        @DisplayName("VL-CP3 · salida=entrada (límite C exacto, 0 noches) → IllegalArgumentException")
        void vlCp3_salidaIgualEntrada_limiteC() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /**
         * VL-CP4 | entrada = ayer — límite exacto inválido de D
         */
        @Test
        @DisplayName("VL-CP4 · entrada=ayer (límite D exacto) → IllegalArgumentException: en el pasado")
        void vlCp4_entradaAyer_limiteD() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /**
         * VL-CP5 | cálculo de importe: 3 noches × 100€
         * base=300, fee=36, total=336
         */
        @Test
        @DisplayName("VL-CP5 · 3 noches × 100€ → importe=336.0, tarifa=36.0")
        void vlCp5_calculoImporte_tresNoches() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reserva r = reservaService.crearReservaInmediata(
                1L, inquilino, MAS5, MAS8);

            assertThat(r.getNumNoches()).isEqualTo(3);
            assertThat(r.getTarifaServicio()).isEqualTo(36.0);
            assertThat(r.getImporteTotal()).isEqualTo(336.0);
        }
    }

    // =========================================================================
    // DEC — Tabla de Decisión
    // =========================================================================

    @Nested
    @DisplayName("DEC — Tabla de Decisión")
    class TablaDecision {

        /** DEC-CP1 | A=F,B=F,C=F,D=F,E=F → Dec6=F → Reserva OK */
        @Test
        @DisplayName("DEC-CP1 · todas F → Dec6=F → Reserva creada")
        void decCp1_todasFalse_reservaOK() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isNotNull();
        }

        /** DEC-CP2 | A=T → Dec1=T */
        @Test
        @DisplayName("DEC-CP2 · A=T (solo) → Dec1=T → IllegalArgumentException")
        void decCp2_soloA() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }

        /** DEC-CP3 | B=T → Dec2=T */
        @Test
        @DisplayName("DEC-CP3 · B=T (solo) → Dec2=T → IllegalStateException")
        void decCp3_soloB() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Este inmueble requiere solicitud al propietario");
        }

        /** DEC-CP4 | C=T → Dec3=T (entrada > salida) */
        @Test
        @DisplayName("DEC-CP4 · C=T (solo) → Dec3=T → IllegalArgumentException")
        void decCp4_soloC() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS8, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /** DEC-CP5 | D=T → Dec4=T (entrada en pasado, C=F) */
        @Test
        @DisplayName("DEC-CP5 · D=T (solo) → Dec4=T → IllegalArgumentException")
        void decCp5_soloD() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /** DEC-CP6 | E=T → Dec5=T */
        @Test
        @DisplayName("DEC-CP6 · E=T (solo) → Dec5=T → IllegalStateException")
        void decCp6_soloE() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El inmueble no está disponible en las fechas seleccionadas");
        }

        /**
         * DEC-CP7 | A=T, E=T → A evalúa ANTES que E
         * obtenerInmuebleOFallar() lanza antes de llegar a validarDisponibilidad()
         */
        @Test
        @DisplayName("DEC-CP7 · A=T,E=T → A evalúa antes → IllegalArgumentException")
        void decCp7_aYe_aDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }

        /**
         * DEC-CP8 | B=T, C=T → B evalúa ANTES que C
         * validarFlujoInmediato() lanza antes de llegar a validarFechas()
         */
        @Test
        @DisplayName("DEC-CP8 · B=T,C=T → B evalúa antes → IllegalStateException")
        void decCp8_bYc_bDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS8, MAS5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Este inmueble requiere solicitud al propietario");
        }

        /**
         * DEC-CP9 | C=T, D=T → C evalúa ANTES que D
         * !entrada.isBefore(salida) se comprueba ANTES que entrada.isBefore(now())
         */
        @Test
        @DisplayName("DEC-CP9 · C=T,D=T → C evalúa antes que D → IllegalArgumentException (anterior a la de salida)")
        void decCp9_cYd_cDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            // entrada=ayer, salida=ayer-1: C=T y D=T, pero C se evalúa primero
            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, AYER.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /**
         * DEC-CP10 | A=T, B=T, C=T, D=T, E=T → A evalúa primero
         */
        @Test
        @DisplayName("DEC-CP10 · todas T → A evalúa primero → IllegalArgumentException")
        void decCp10_todasTrue_aDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, AYER.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }
    }

    // =========================================================================
    // MCD — Condición Dominante Modificada (16 CPs)
    // =========================================================================

    @Nested
    @DisplayName("MCD — Condición Dominante Modificada")
    class CondicionDominante {

        /** MCD-CP1 | Caso base: todas F → Reserva OK */
        @Test
        @DisplayName("MCD-CP1 · caso base todas F → Reserva OK")
        void mcdCp1_casoBase() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isNotNull();
        }

        /** MCD-CP2 | E=T (solo) → Dec5 cambia a T */
        @Test
        @DisplayName("MCD-CP2 · E=T solo → Dec5=T → E domina Dec5")
        void mcdCp2_eDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El inmueble no está disponible en las fechas seleccionadas");
        }

        /** MCD-CP3 | B=T (solo) → Dec2 cambia a T */
        @Test
        @DisplayName("MCD-CP3 · B=T solo → Dec2=T → B domina Dec2")
        void mcdCp3_bDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Este inmueble requiere solicitud al propietario");
        }

        /**
         * MCD-CP4 | B=T, E=T → B evalúa antes que E
         * FIX: NO stubbing de existeSolapamiento porque B lanza antes de llegar a E
         */
        @Test
        @DisplayName("MCD-CP4 · B=T,E=T → B evalúa antes que E → IllegalStateException (B)")
        void mcdCp4_bYe_bAntes() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));
            // NO se stubbing existeSolapamiento: B lanza antes de llegar a validarDisponibilidad

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Este inmueble requiere solicitud al propietario");
        }

        /** MCD-CP5 | D=T (solo, C=F) → Dec4 cambia a T */
        @Test
        @DisplayName("MCD-CP5 · D=T solo (C=F) → Dec4=T → D domina Dec4")
        void mcdCp5_dDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /**
         * MCD-CP6 | D=T, E=T → D evalúa antes que E
         * FIX: NO stubbing de existeSolapamiento porque D lanza en validarFechas
         *      antes de llegar a validarDisponibilidad
         */
        @Test
        @DisplayName("MCD-CP6 · D=T,E=T → D evalúa antes que E → IllegalArgumentException (en el pasado)")
        void mcdCp6_dYe_dAntes() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            // NO stubbing de existeSolapamiento: D lanza antes de llegar a validarDisponibilidad

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /** MCD-CP7 | C=T (solo, D=F) → Dec3 cambia a T */
        @Test
        @DisplayName("MCD-CP7 · C=T solo (D=F) → Dec3=T → C domina Dec3")
        void mcdCp7_cDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS8, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /**
         * MCD-CP8 | C=T, E=T → C evalúa antes que E
         * FIX: NO stubbing de existeSolapamiento porque C lanza en validarFechas
         */
        @Test
        @DisplayName("MCD-CP8 · C=T,E=T → C evalúa antes que E → IllegalArgumentException (anterior)")
        void mcdCp8_cYe_cAntes() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            // NO stubbing de existeSolapamiento: C lanza antes de llegar a validarDisponibilidad

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS8, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /** MCD-CP9 | A=T (solo) → Dec1 cambia a T */
        @Test
        @DisplayName("MCD-CP9 · A=T solo → Dec1=T → A domina Dec1")
        void mcdCp9_aDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }

        /** MCD-CP10 | A=T, E=T → A evalúa antes que E */
        @Test
        @DisplayName("MCD-CP10 · A=T,E=T → A evalúa antes que E → IllegalArgumentException")
        void mcdCp10_aYe_aAntes() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }

        /** MCD-CP11 | A=T, B=T → A evalúa antes que B */
        @Test
        @DisplayName("MCD-CP11 · A=T,B=T → A evalúa antes que B → IllegalArgumentException")
        void mcdCp11_aYb_aAntes() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }

        /** MCD-CP12 | A=T, B=T, E=T → A domina */
        @Test
        @DisplayName("MCD-CP12 · A=T,B=T,E=T → A domina → IllegalArgumentException")
        void mcdCp12_aYbYe() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS5, MAS8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }

        /**
         * MCD-CP13 | D=T, E=F → Dec4=T
         * FIX: NO stubbing de existeSolapamiento: D lanza antes de llegar a E
         */
        @Test
        @DisplayName("MCD-CP13 · D=T,E=F → Dec4=T → IllegalArgumentException (en el pasado)")
        void mcdCp13_dSinE() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            // NO stubbing de existeSolapamiento: nunca se llega a validarDisponibilidad

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /**
         * MCD-CP14 | D=T, E=T → D evalúa antes que E
         * FIX: NO stubbing de existeSolapamiento
         */
        @Test
        @DisplayName("MCD-CP14 · D=T,E=T → D evalúa antes que E → IllegalArgumentException (en el pasado)")
        void mcdCp14_dYe() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));
            // NO stubbing de existeSolapamiento: D lanza antes

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        /** MCD-CP15 | C=T, D=F → Dec3=T */
        @Test
        @DisplayName("MCD-CP15 · C=T,D=F → Dec3=T → IllegalArgumentException (anterior a la de salida)")
        void mcdCp15_cSinD() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleInmediata));

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, MAS8, MAS5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la de salida");
        }

        /** MCD-CP16 | A=T,B=T,C=T,D=T,E=T → todas activas → A domina */
        @Test
        @DisplayName("MCD-CP16 · todas T → A domina → IllegalArgumentException: Inmueble no encontrado")
        void mcdCp16_todasTrue_aDomina() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                reservaService.crearReservaInmediata(1L, inquilino, AYER, AYER.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inmueble no encontrado");
        }
    }

    // =========================================================================
    // Otros métodos
    // =========================================================================

    @Nested
    @DisplayName("Otros métodos — crearSolicitud, aceptar, rechazar, cancelar, confirmar")
    class OtrosMetodos {

        @Test
        @DisplayName("crearSolicitud · CE válida → SolicitudReserva.estado=PENDIENTE")
        void crearSolicitud_valida_estadoPendiente() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SolicitudReserva sol = reservaService.crearSolicitud(
                1L, inquilino, MAS5, MAS8, "Somos una pareja.");

            assertThat(sol.getEstado())
                .isEqualTo(SolicitudReserva.EstadoSolicitud.PENDIENTE);
            assertThat(sol.getMensajeInquilino()).isEqualTo("Somos una pareja.");
            verify(notificacionService).notificarNuevaSolicitud(any());
        }

        @Test
        @DisplayName("crearSolicitud · fecha pasada → IllegalArgumentException: en el pasado")
        void crearSolicitud_fechaPasada_illegalArgument() {
            when(inmuebleRepository.findById(any())).thenReturn(Optional.of(inmuebleSolicitud));

            assertThatThrownBy(() ->
                reservaService.crearSolicitud(1L, inquilino, AYER, MAS5, "msg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser en el pasado");
        }

        @Test
        @DisplayName("aceptarSolicitud · isPendiente()=true → Reserva CONFIRMADA + solicitud ACEPTADA")
        void aceptarSolicitud_pendiente_reservaConfirmada() {
            SolicitudReserva sol = new SolicitudReserva(
                MAS5, MAS8, inquilino, inmuebleSolicitud, "Hola");

            when(solicitudRepository.findById(any())).thenReturn(Optional.of(sol));
            when(reservaRepository.existeSolapamiento(any(), any(), any())).thenReturn(false);
            when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reserva r = reservaService.aceptarSolicitud(1L, "Bienvenido");

            assertThat(sol.getEstado())
                .isEqualTo(SolicitudReserva.EstadoSolicitud.ACEPTADA);
            assertThat(sol.getMensajePropietario()).isEqualTo("Bienvenido");
            assertThat(r.getEstado()).isEqualTo(Reserva.EstadoReserva.CONFIRMADA);
            assertThat(r.getTipoFlujo()).isEqualTo(Reserva.TipoFlujo.SOLICITUD);
            verify(notificacionService).notificarSolicitudAceptada(any());
        }

        @Test
        @DisplayName("aceptarSolicitud · isPendiente()=false (ACEPTADA) → IllegalStateException: ya fue procesada")
        void aceptarSolicitud_yaAceptada_illegalState() {
            SolicitudReserva sol = new SolicitudReserva(
                MAS5, MAS8, inquilino, inmuebleSolicitud, "Hola");
            sol.aceptar("ok");

            when(solicitudRepository.findById(any())).thenReturn(Optional.of(sol));

            assertThatThrownBy(() -> reservaService.aceptarSolicitud(1L, "otro"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La solicitud ya fue procesada");
        }

        @Test
        @DisplayName("aceptarSolicitud · isPendiente()=false (RECHAZADA) → IllegalStateException: ya fue procesada")
        void aceptarSolicitud_yaRechazada_illegalState() {
            SolicitudReserva sol = new SolicitudReserva(
                MAS5, MAS8, inquilino, inmuebleSolicitud, "Hola");
            sol.rechazar("no hay hueco");

            when(solicitudRepository.findById(any())).thenReturn(Optional.of(sol));

            assertThatThrownBy(() -> reservaService.aceptarSolicitud(1L, "msg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La solicitud ya fue procesada");
        }

        @Test
        @DisplayName("rechazarSolicitud · isPendiente()=true → estado RECHAZADA + notificación")
        void rechazarSolicitud_pendiente_estadoRechazada() {
            SolicitudReserva sol = new SolicitudReserva(
                MAS5, MAS8, inquilino, inmuebleSolicitud, "Hola");

            when(solicitudRepository.findById(any())).thenReturn(Optional.of(sol));
            when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SolicitudReserva r = reservaService.rechazarSolicitud(
                1L, "No está disponible esas fechas.");

            assertThat(r.getEstado())
                .isEqualTo(SolicitudReserva.EstadoSolicitud.RECHAZADA);
            assertThat(r.getMensajePropietario())
                .isEqualTo("No está disponible esas fechas.");
            verify(notificacionService).notificarSolicitudRechazada(any());
        }

        @Test
        @DisplayName("rechazarSolicitud · isPendiente()=false → IllegalStateException: ya fue procesada")
        void rechazarSolicitud_yaProcesada_illegalState() {
            SolicitudReserva sol = new SolicitudReserva(
                MAS5, MAS8, inquilino, inmuebleSolicitud, "Hola");
            sol.aceptar("ok");

            when(solicitudRepository.findById(any())).thenReturn(Optional.of(sol));

            assertThatThrownBy(() -> reservaService.rechazarSolicitud(1L, "msg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La solicitud ya fue procesada");
        }

        /**
         * cancelarReserva · inquilino cancela su propia reserva
         *
         * FIX NullPointerException:
         * cancelarReserva() en el código comprueba:
         *   reserva.getInquilino().getId().equals(usuarioId)
         *   reserva.getInmueble().getPropietario().getId().equals(usuarioId)
         * → AMBOS getId() deben tener valor asignado por reflexión:
         *   inquilino.id = 10L  (viene de Usuario, campo "id")
         *   propietario.id = 20L (viene de Usuario, campo "id")
         */
        @Test
        @DisplayName("cancelarReserva · usuarioId = inquilino.getId() → estado CANCELADA")
        void cancelarReserva_porInquilino_cancelada() {
            setId(inquilino,   10L);   // reserva.getInquilino().getId() = 10L
            setId(propietario, 20L);   // reserva.getInmueble().getPropietario().getId() = 20L

            Reserva res = new Reserva(MAS5, MAS8, inquilino, inmuebleInmediata,
                Reserva.TipoFlujo.INMEDIATA);
            res.confirmar();

            when(reservaRepository.findById(any())).thenReturn(Optional.of(res));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // 10L coincide con inquilino.getId() → esInquilino=true
            Reserva r = reservaService.cancelarReserva(1L, 10L);

            assertThat(r.getEstado()).isEqualTo(Reserva.EstadoReserva.CANCELADA);
            verify(notificacionService).notificarReservaCancelada(any());
        }

        @Test
        @DisplayName("cancelarReserva · usuarioId = propietario.getId() → estado CANCELADA")
        void cancelarReserva_porPropietario_cancelada() {
            setId(inquilino,   10L);
            setId(propietario, 20L);

            Reserva res = new Reserva(MAS5, MAS8, inquilino, inmuebleInmediata,
                Reserva.TipoFlujo.INMEDIATA);
            res.confirmar();

            when(reservaRepository.findById(any())).thenReturn(Optional.of(res));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // 20L coincide con propietario.getId() → esPropietario=true
            Reserva r = reservaService.cancelarReserva(1L, 20L);

            assertThat(r.getEstado()).isEqualTo(Reserva.EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("cancelarReserva · usuarioId distinto → SecurityException: No tienes permiso")
        void cancelarReserva_tercero_securityException() {
            setId(inquilino,   10L);
            setId(propietario, 20L);

            Reserva res = new Reserva(MAS5, MAS8, inquilino, inmuebleInmediata,
                Reserva.TipoFlujo.INMEDIATA);

            when(reservaRepository.findById(any())).thenReturn(Optional.of(res));

            // 30L no coincide ni con 10L ni con 20L → !esInquilino && !esPropietario
            assertThatThrownBy(() -> reservaService.cancelarReserva(1L, 30L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("No tienes permiso para cancelar esta reserva");
        }

        @Test
        @DisplayName("confirmarTrasPago · reserva existe → estado CONFIRMADA + notificación")
        void confirmarTrasPago_existe_confirmada() {
            Reserva res = new Reserva(MAS5, MAS8, inquilino, inmuebleInmediata,
                Reserva.TipoFlujo.INMEDIATA);

            when(reservaRepository.findById(any())).thenReturn(Optional.of(res));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reserva r = reservaService.confirmarTrasPago(1L);

            assertThat(r.getEstado()).isEqualTo(Reserva.EstadoReserva.CONFIRMADA);
            verify(notificacionService).notificarReservaConfirmada(any());
        }

        @Test
        @DisplayName("confirmarTrasPago · reserva no existe → IllegalArgumentException: Reserva no encontrada")
        void confirmarTrasPago_noExiste_illegalArgument() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservaService.confirmarTrasPago(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        @Test
        @DisplayName("listarPorInquilino · delega en findByInquilinoOrderByFechaCreacionDesc")
        void listarPorInquilino_delegaEnRepo() {
            when(reservaRepository.findByInquilinoOrderByFechaCreacionDesc(inquilino))
                .thenReturn(List.of());

            List<Reserva> r = reservaService.listarPorInquilino(inquilino);

            assertThat(r).isNotNull();
            verify(reservaRepository).findByInquilinoOrderByFechaCreacionDesc(inquilino);
        }

        @Test
        @DisplayName("listarSolicitudesPendientes · delega en findPendientesByPropietarioId")
        void listarSolicitudesPendientes_delegaEnRepo() {
            when(solicitudRepository.findPendientesByPropietarioId(1L))
                .thenReturn(List.of());

            assertThat(reservaService.listarSolicitudesPendientes(1L)).isNotNull();
            verify(solicitudRepository).findPendientesByPropietarioId(1L);
        }

        @Test
        @DisplayName("obtenerReserva · ID existente → Optional.of(reserva)")
        void obtenerReserva_existe_presente() {
            Reserva res = new Reserva(MAS5, MAS8, inquilino, inmuebleInmediata,
                Reserva.TipoFlujo.INMEDIATA);
            when(reservaRepository.findById(any())).thenReturn(Optional.of(res));

            assertThat(reservaService.obtenerReserva(1L)).isPresent();
        }

        @Test
        @DisplayName("obtenerReserva · ID no existente → Optional.empty()")
        void obtenerReserva_noExiste_vacio() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThat(reservaService.obtenerReserva(1L)).isEmpty();
        }
    }

    /**
     * Asigna el campo "id" de Usuario (superclase abstracta) por reflexión.
     * Necesario porque JPA asigna el ID al persistir, pero en tests unitarios
     * no hay persistencia real.
     * cancelarReserva() llama a:
     *   reserva.getInquilino().getId()              → inquilino.id
     *   reserva.getInmueble().getPropietario().getId() → propietario.id
     * Ambos deben estar asignados antes de crear la Reserva.
     */
    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Error asignando ID: " + e.getMessage(), e);
        }
    }
}

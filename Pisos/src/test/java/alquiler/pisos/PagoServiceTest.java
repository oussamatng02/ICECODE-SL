package alquiler.pisos;



import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import alquiler.pisos.entity.*;
import alquiler.pisos.repository.*;
import alquiler.pisos.service.*;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de PagoService basados en el código real.
 *
 * Métodos analizados:
 *   iniciarPago(Long reservaId, Pago.MetodoPago metodo)
 *   confirmarPago(Long pagoId)
 *   procesarReembolso(Long reservaId)
 *
 * Mensajes exactos del código:
 *   "Reserva no encontrada: " + reservaId
 *   "La reserva no está pendiente de pago"
 *   "Pago no encontrado: " + pagoId
 *   "No existe pago para esta reserva"
 *   "El pago no está en estado completado"
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService — CE + VL + Decisión + MCD")
class PagoServiceTest {

    @Mock private PagoRepository    pagoRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private ReservaService    reservaService;

    @InjectMocks private PagoService pagoService;

    private Propietario propietario;
    private Inquilino   inquilino;
    private Inmueble    inmueble;
    private Reserva     reservaPendiente;
    private Reserva     reservaConfirmada;

    private static final LocalDate MAS5 = LocalDate.now().plusDays(5);
    private static final LocalDate MAS8 = LocalDate.now().plusDays(8);

    @BeforeEach
    void setUp() {
        propietario = new Propietario("Carlos", "carlos@test.com", "pass");
        inquilino   = new Inquilino("Ana",     "ana@test.com",    "pass");
        inmueble    = new Inmueble("Ático", "Desc", "Madrid",
            Inmueble.TipoInmueble.VIVIENDA_COMPLETA, 100.0, propietario);
        inmueble.setTipoFlujo(Inmueble.TipoFlujoReserva.INMEDIATA);

        reservaPendiente = new Reserva(MAS5, MAS8, inquilino, inmueble,
            Reserva.TipoFlujo.INMEDIATA);
        // estado=PENDIENTE_PAGO por defecto, importeTotal=336.0

        reservaConfirmada = new Reserva(MAS5, MAS8, inquilino, inmueble,
            Reserva.TipoFlujo.INMEDIATA);
        reservaConfirmada.confirmar();
    }

    // =========================================================================
    // CE — Clases de Equivalencia [iniciarPago]
    // =========================================================================

    @Nested
    @DisplayName("CE iniciarPago — Clases de Equivalencia")
    class CeIniciarPago {

        /**
         * CP1 | CE válida: reserva existe + PENDIENTE_PAGO + método válido
         * → Pago creado con estado=PENDIENTE e importe=336.0
         */
        @Test
        @DisplayName("CP1 · CE válida · reserva OK + PENDIENTE_PAGO + TARJETA_CREDITO → Pago PENDIENTE")
        void cp1_todosValidos_pagoCreado() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago p = pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO);

            assertThat(p.getEstado()).isEqualTo(Pago.EstadoPago.PENDIENTE);
            assertThat(p.getMetodo()).isEqualTo(Pago.MetodoPago.TARJETA_CREDITO);
            assertThat(p.getImporte()).isEqualTo(336.0);
            assertThat(p.getReserva()).isEqualTo(reservaPendiente);
            verify(pagoRepository).save(any());
        }

        /**
         * CP2 | A=T — findById devuelve Optional.empty()
         * "Reserva no encontrada: " + reservaId
         */
        @Test
        @DisplayName("CP2 · CE inválida · A=T (findById vacío) → IllegalArgumentException: Reserva no encontrada")
        void cp2_reservaNoExiste_illegalArgument() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");

            verify(pagoRepository, never()).save(any());
        }

        /**
         * CP3 | B=T — estado=CONFIRMADA ≠ PENDIENTE_PAGO
         * "La reserva no está pendiente de pago"
         */
        @Test
        @DisplayName("CP3 · CE inválida · B=T (CONFIRMADA) → IllegalStateException: no está pendiente de pago")
        void cp3_reservaConfirmada_illegalState() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaConfirmada));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");

            verify(pagoRepository, never()).save(any());
        }

        /**
         * CP4-CP5 | CE válida: los 3 métodos de pago son válidos
         * @ParameterizedTest: un test por cada valor del enum MetodoPago
         * Sonar: sustituye 3 tests idénticos por uno parametrizado
         */
        @ParameterizedTest(name = "método={0} → Pago creado OK")
        @EnumSource(Pago.MetodoPago.class)
        @DisplayName("CP4-CP5 · CE válida · todos los MetodoPago → Pago creado")
        void cp4cp5_todosMetodosValidos_pagoCreado(Pago.MetodoPago metodo) {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago p = pagoService.iniciarPago(1L, metodo);

            assertThat(p.getMetodo()).isEqualTo(metodo);
            assertThat(p.getEstado()).isEqualTo(Pago.EstadoPago.PENDIENTE);
        }

        /**
         * CP6 | B=T — estado=CANCELADA ≠ PENDIENTE_PAGO
         */
        @Test
        @DisplayName("CP6 · CE inválida · B=T (CANCELADA) → IllegalStateException")
        void cp6_reservaCancelada_illegalState() {
            reservaPendiente.cancelar();
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }
    }

    // =========================================================================
    // CE — Clases de Equivalencia [confirmarPago]
    // =========================================================================

    @Nested
    @DisplayName("CE confirmarPago — Clases de Equivalencia")
    class CeConfirmarPago {

        /**
         * CP7 | CE válida: pago existe
         * → estado=COMPLETADO, referenciaExterna="TXN-XXXXXXXX"
         * → reservaService.confirmarTrasPago() llamado
         */
        @Test
        @DisplayName("CP7 · CE válida · pago existe → estado=COMPLETADO, referenciaExterna asignada")
        void cp7_pagoExiste_completado() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            when(pagoRepository.findById(any())).thenReturn(Optional.of(pago));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago resultado = pagoService.confirmarPago(1L);

            assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.COMPLETADO);
            assertThat(resultado.getReferenciaExterna())
                .isNotNull()
                .startsWith("TXN-")
                .hasSize(12);
            verify(reservaService).confirmarTrasPago(any());
        }

        /**
         * CP8 | CE inválida: findById devuelve Optional.empty()
         * "Pago no encontrado: " + pagoId
         */
        @Test
        @DisplayName("CP8 · CE inválida · pago no existe → IllegalArgumentException: Pago no encontrado")
        void cp8_pagoNoExiste_illegalArgument() {
            when(pagoRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.confirmarPago(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pago no encontrado");

            verify(reservaService, never()).confirmarTrasPago(any());
        }
    }

    // =========================================================================
    // CE — Clases de Equivalencia [procesarReembolso]
    // =========================================================================

    @Nested
    @DisplayName("CE procesarReembolso — Clases de Equivalencia")
    class CeProcesarReembolso {

        /**
         * CP9 | CE válida: reserva existe + pago existe + estado=COMPLETADO
         * → estado=REEMBOLSADO, fechaReembolso asignada
         */
        @Test
        @DisplayName("CP9 · CE válida · pago COMPLETADO → estado=REEMBOLSADO")
        void cp9_pagoCompletado_reembolsado() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            pago.completar("TXN-12345678");

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago resultado = pagoService.procesarReembolso(1L);

            assertThat(resultado.getEstado()).isEqualTo(Pago.EstadoPago.REEMBOLSADO);
            assertThat(resultado.getFechaReembolso()).isNotNull();
        }

        /**
         * CP10 | C=T — reserva no existe
         * "Reserva no encontrada: " + reservaId
         */
        @Test
        @DisplayName("CP10 · CE inválida · C=T (reserva no existe) → IllegalArgumentException")
        void cp10_reservaNoExiste_illegalArgument() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");

            verify(pagoRepository, never()).findByReserva(any());
        }

        /**
         * CP11 | D=T — findByReserva devuelve Optional.empty()
         * "No existe pago para esta reserva"
         */
        @Test
        @DisplayName("CP11 · CE inválida · D=T (findByReserva vacío) → IllegalStateException: No existe pago")
        void cp11_pagoNoExiste_illegalState() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe pago para esta reserva");
        }

        /**
         * CP12 | E=T — estado=PENDIENTE ≠ COMPLETADO
         * "El pago no está en estado completado"
         */
        @Test
        @DisplayName("CP12 · CE inválida · E=T (PENDIENTE) → IllegalStateException: no está en estado completado")
        void cp12_pagoPendiente_illegalState() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }
    }

    // =========================================================================
    // VL — Valores Límite
    // =========================================================================

    @Nested
    @DisplayName("VL — Valores Límite")
    class ValoresLimite {

        /**
         * VL-CP1 | importe del Pago = reserva.getImporteTotal() exacto
         * 3 noches × 100€ → base=300, fee=36, total=336.0
         */
        @Test
        @DisplayName("VL-CP1 · importe pago = reserva.importeTotal (336.0€) → asignado exacto")
        void vlCp1_importeExactoDeReserva() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago p = pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO);

            assertThat(p.getImporte()).isEqualTo(336.0);
        }

        /**
         * VL-CP2 | reserva.estado=COMPLETADA → B=T
         */
        @Test
        @DisplayName("VL-CP2 · reserva.estado=COMPLETADA → IllegalStateException")
        void vlCp2_reservaCompletada_illegalState() {
            reservaPendiente.completar();
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /**
         * VL-CP3 | estados inválidos de pago para reembolso (PENDIENTE, FALLIDO, REEMBOLSADO)
         * @ParameterizedTest: un test por cada estado inválido
         * Sonar: sustituye 3 tests idénticos por uno parametrizado
         */
        @ParameterizedTest(name = "pago.estado={0} → IllegalStateException")
        @EnumSource(value = Pago.EstadoPago.class,
                    names = {"PENDIENTE", "FALLIDO", "REEMBOLSADO"})
        @DisplayName("VL-CP3 · E=T (PENDIENTE/FALLIDO/REEMBOLSADO) → IllegalStateException: no está en estado completado")
        void vlCp3_estadosInvalidos_illegalState(Pago.EstadoPago estadoInvalido) {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            // Poner el estado inválido correspondiente
            switch (estadoInvalido) {
                case FALLIDO     -> pago.marcarFallido();
                case REEMBOLSADO -> { pago.completar("TXN-X"); pago.reembolsar(); }
                default          -> { /* PENDIENTE es el estado por defecto */ }
            }

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }

        /**
         * VL-CP4 | referenciaExterna generada con formato "TXN-" + 8 chars
         */
        @Test
        @DisplayName("VL-CP4 · referenciaExterna = 'TXN-' + 8 chars → longitud=12, formato correcto")
        void vlCp4_referenciaExternaFormato() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.PAYPAL, reservaPendiente);
            when(pagoRepository.findById(any())).thenReturn(Optional.of(pago));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago resultado = pagoService.confirmarPago(1L);

            assertThat(resultado.getReferenciaExterna())
                .startsWith("TXN-")
                .hasSize(12)
                .matches("TXN-[A-F0-9]{8}");
        }
    }

    // =========================================================================
    // DEC — Tabla de Decisión [iniciarPago]
    // =========================================================================

    @Nested
    @DisplayName("DEC iniciarPago — Tabla de Decisión")
    class TablaDecision {

        /** DEC-CP1 | A=F, B=F → Dec3=F → Pago creado */
        @Test
        @DisplayName("DEC-CP1 · A=F,B=F → Dec3=F → Pago creado")
        void decCp1_todasFalse_pagoOK() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isNotNull();
        }

        /** DEC-CP2 | A=T → Dec1=T */
        @Test
        @DisplayName("DEC-CP2 · A=T (solo) → Dec1=T → IllegalArgumentException")
        void decCp2_soloA() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /**
         * DEC-CP3 a DEC-CP5 | B=T con todos los estados inválidos
         * @ParameterizedTest: sustituye 3 tests idénticos (CONFIRMADA, CANCELADA, COMPLETADA)
         */
        @ParameterizedTest(name = "reserva.estado={0} → IllegalStateException")
        @EnumSource(value = Reserva.EstadoReserva.class,
                    names = {"CONFIRMADA", "CANCELADA", "COMPLETADA"})
        @DisplayName("DEC-CP3-CP5 · B=T (CONFIRMADA/CANCELADA/COMPLETADA) → IllegalStateException")
        void decCp3Cp5_bTrue_todosEstadosInvalidos(Reserva.EstadoReserva estadoInvalido) {
            switch (estadoInvalido) {
                case CONFIRMADA -> reservaPendiente.confirmar();
                case CANCELADA  -> reservaPendiente.cancelar();
                case COMPLETADA -> reservaPendiente.completar();
                default -> {}
            }
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /**
         * DEC-CP6 | A=T, B=T → A evalúa ANTES que B
         * findById vacío → IllegalArgumentException (A domina)
         */
        @Test
        @DisplayName("DEC-CP6 · A=T,B=T → A evalúa antes → IllegalArgumentException")
        void decCp6_aYb_aDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }
    }

    // =========================================================================
    // MCD — Condición Dominante Modificada (16 CPs)
    // =========================================================================

    @Nested
    @DisplayName("MCD — Condición Dominante Modificada")
    class CondicionDominante {

        /** MCD-CP1 | Caso base iniciarPago: A=F, B=F → Pago OK */
        @Test
        @DisplayName("MCD-CP1 · caso base iniciarPago (A=F,B=F) → Pago creado")
        void mcdCp1_casoBaseIniciar() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isNotNull();
        }

        /** MCD-CP2 | Caso base procesarReembolso: C=F, D=F, E=F → Reembolso OK */
        @Test
        @DisplayName("MCD-CP2 · caso base procesarReembolso (C=F,D=F,E=F) → REEMBOLSADO")
        void mcdCp2_casoBaseReembolso() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            pago.completar("TXN-12345678");

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(pagoService.procesarReembolso(1L).getEstado())
                .isEqualTo(Pago.EstadoPago.REEMBOLSADO);
        }

        /** MCD-CP3 | B=T (solo) → B domina Dec2 */
        @Test
        @DisplayName("MCD-CP3 · B=T solo → B domina Dec2 → IllegalStateException")
        void mcdCp3_bDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaConfirmada));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /**
         * MCD-CP4, CP5, CP16 | A=T domina en iniciarPago
         * Todos hacen findById=empty → misma aserción
         * @ParameterizedTest: elimina duplicados que detectó SonarQube
         */
        @ParameterizedTest(name = "escenario A=T #{index}: findById vacío → A domina")
        @org.junit.jupiter.params.provider.ValueSource(
            strings = {"CP4-A-solo", "CP5-A+B", "CP16-todas-T"})
        @DisplayName("MCD-CP4/CP5/CP16 · A=T → A domina → IllegalArgumentException: Reserva no encontrada")
        void mcdCp4Cp5Cp16_aDomina(String escenario) {
            // En todos los escenarios A=T: findById devuelve vacío
            // B, C, D, E no se evalúan porque A lanza primero
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /** MCD-CP6 | C=T (solo) → C domina en procesarReembolso */
        @Test
        @DisplayName("MCD-CP6 · C=T solo → C domina → IllegalArgumentException")
        void mcdCp6_cDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /** MCD-CP7 | D=T (solo) → D domina en procesarReembolso */
        @Test
        @DisplayName("MCD-CP7 · D=T solo → D domina → IllegalStateException: No existe pago")
        void mcdCp7_dDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe pago para esta reserva");
        }

        /** MCD-CP8 | E=T (solo) → E domina en procesarReembolso */
        @Test
        @DisplayName("MCD-CP8 · E=T solo → E domina → IllegalStateException: no está en estado completado")
        void mcdCp8_eDomina() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }

        /**
         * MCD-CP9, CP10 | C=T, D=T / C=T,D=T,E=T → C evalúa primero
         * @ParameterizedTest: escenarios donde C siempre domina
         */
        @ParameterizedTest(name = "escenario C=T #{index} → C evalúa primero")
        @org.junit.jupiter.params.provider.ValueSource(
            strings = {"CP9-C+D", "CP10-C+D+E"})
        @DisplayName("MCD-CP9/CP10 · C=T → C evalúa antes que D,E → IllegalArgumentException")
        void mcdCp9Cp10_cDomina(String escenario) {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /**
         * MCD-CP11 | D=T, E=T → D evalúa antes que E
         * findByReserva vacío → D lanza antes de comprobar pago.estado
         */
        @Test
        @DisplayName("MCD-CP11 · D=T,E=T → D evalúa antes que E → IllegalStateException: No existe pago")
        void mcdCp11_dYe_dAntes() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe pago para esta reserva");
        }

        /**
         * MCD-CP12, CP13, CP14 | B=T con variantes de estado
         * @ParameterizedTest: sustituye los 3 tests idénticos de B
         */
        @ParameterizedTest(name = "B=T estado={0} → IllegalStateException")
        @EnumSource(value = Reserva.EstadoReserva.class,
                    names = {"CONFIRMADA", "CANCELADA", "COMPLETADA"})
        @DisplayName("MCD-CP12/CP13/CP14 · B=T variantes → IllegalStateException: no está pendiente")
        void mcdCp12Cp13Cp14_bVariantes(Reserva.EstadoReserva estado) {
            switch (estado) {
                case CONFIRMADA -> reservaPendiente.confirmar();
                case CANCELADA  -> reservaPendiente.cancelar();
                case COMPLETADA -> reservaPendiente.completar();
                default -> {}
            }
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /**
         * MCD-CP15 | E=T con FALLIDO
         */
        @Test
        @DisplayName("MCD-CP15 · E=T (FALLIDO) → IllegalStateException: no está en estado completado")
        void mcdCp15_eFallido() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_DEBITO, reservaPendiente);
            pago.marcarFallido();

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }
    }

    // =========================================================================
    // obtenerPagoPorReserva
    // =========================================================================

    @Nested
    @DisplayName("obtenerPagoPorReserva")
    class ObtenerPago {

        @Test
        @DisplayName("pago existe → Optional.of(pago)")
        void pagoExiste_presente() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            when(pagoRepository.findByReserva(reservaPendiente)).thenReturn(Optional.of(pago));

            assertThat(pagoService.obtenerPagoPorReserva(reservaPendiente)).isPresent();
        }

        @Test
        @DisplayName("pago no existe → Optional.empty()")
        void pagoNoExiste_vacio() {
            when(pagoRepository.findByReserva(reservaPendiente)).thenReturn(Optional.empty());

            assertThat(pagoService.obtenerPagoPorReserva(reservaPendiente)).isEmpty();
        }
    }
}
package Alquieler.Pisos;


import Alquieler.Pisos.entity.*;
import Alquieler.Pisos.service.*;
import Alquieler.Pisos.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
 *   "Reserva no encontrada: " + reservaId   → iniciarPago y procesarReembolso
 *   "La reserva no está pendiente de pago"  → iniciarPago
 *   "Pago no encontrado: " + pagoId         → confirmarPago
 *   "No existe pago para esta reserva"      → procesarReembolso
 *   "El pago no está en estado completado"  → procesarReembolso
 *
 * Condiciones de iniciarPago (orden exacto):
 *   A: reservaRepository.findById().isEmpty()  → IllegalArgumentException
 *   B: reserva.estado != PENDIENTE_PAGO        → IllegalStateException
 *
 * Condiciones de procesarReembolso (orden exacto):
 *   C: reservaRepository.findById().isEmpty()        → IllegalArgumentException
 *   D: pagoRepository.findByReserva().isEmpty()      → IllegalStateException
 *   E: pago.estado != COMPLETADO                     → IllegalStateException
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService — CE + VL + Decisión + MCD")
class PagoServiceTest {

    @Mock private PagoRepository    pagoRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private ReservaService    reservaService;

    @InjectMocks private PagoService pagoService;

    // ── Fixtures con constructores reales ─────────────────────────────────────
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

        // Reserva con estado inicial = PENDIENTE_PAGO
        reservaPendiente = new Reserva(MAS5, MAS8, inquilino, inmueble,
            Reserva.TipoFlujo.INMEDIATA);
        // estado=PENDIENTE_PAGO por defecto, importeTotal=336.0 (100*3+12%)

        // Reserva confirmada
        reservaConfirmada = new Reserva(MAS5, MAS8, inquilino, inmueble,
            Reserva.TipoFlujo.INMEDIATA);
        reservaConfirmada.confirmar();
    }

    // =========================================================================
    // CE — Clases de Equivalencia  [iniciarPago]
    // =========================================================================

    @Nested
    @DisplayName("CE iniciarPago — Clases de Equivalencia")
    class CeIniciarPago {

        /**
         * CP1 | CE válida: reserva existe + estado=PENDIENTE_PAGO + método válido
         * → Pago creado con estado=PENDIENTE e importe = reserva.getImporteTotal()
         */
        @Test
        @DisplayName("CP1 · CE válida · reserva OK + PENDIENTE_PAGO + TARJETA_CREDITO → Pago PENDIENTE creado")
        void cp1_todosValidos_pagoCreado() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago p = pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO);

            assertThat(p.getEstado()).isEqualTo(Pago.EstadoPago.PENDIENTE);
            assertThat(p.getMetodo()).isEqualTo(Pago.MetodoPago.TARJETA_CREDITO);
            assertThat(p.getImporte()).isEqualTo(reservaPendiente.getImporteTotal());
            assertThat(p.getReserva()).isEqualTo(reservaPendiente);
            verify(pagoRepository).save(any());
        }

        /**
         * CP2 | A=T — findById devuelve Optional.empty()
         * Mensaje exacto: "Reserva no encontrada: " + reservaId
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
         * Mensaje exacto: "La reserva no está pendiente de pago"
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
         * CP4 | CE válida: método = TARJETA_DEBITO
         */
        @Test
        @DisplayName("CP4 · CE válida · método=TARJETA_DEBITO → Pago creado con metodo=TARJETA_DEBITO")
        void cp4_metodTarjetaDebito_pagoCreado() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago p = pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_DEBITO);

            assertThat(p.getMetodo()).isEqualTo(Pago.MetodoPago.TARJETA_DEBITO);
        }

        /**
         * CP5 | CE válida: método = PAYPAL
         */
        @Test
        @DisplayName("CP5 · CE válida · método=PAYPAL → Pago creado con metodo=PAYPAL")
        void cp5_metodoPaypal_pagoCreado() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Pago p = pagoService.iniciarPago(1L, Pago.MetodoPago.PAYPAL);

            assertThat(p.getMetodo()).isEqualTo(Pago.MetodoPago.PAYPAL);
        }

        /**
         * CP6 | B=T — estado=CANCELADA ≠ PENDIENTE_PAGO
         */
        @Test
        @DisplayName("CP6 · CE inválida · B=T (CANCELADA) → IllegalStateException: no está pendiente de pago")
        void cp6_reservaCancelada_illegalState() {
            reservaPendiente.cancelar(); // estado → CANCELADA
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }
    }

    // =========================================================================
    // CE — Clases de Equivalencia  [confirmarPago]
    // =========================================================================

    @Nested
    @DisplayName("CE confirmarPago — Clases de Equivalencia")
    class CeConfirmarPago {

        /**
         * CP7 | CE válida: pago existe
         * → pago.completar() llamado: estado=COMPLETADO, referenciaExterna="TXN-XXXXXXXX"
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
                .hasSize(12); // "TXN-" + 8 chars
            verify(reservaService).confirmarTrasPago(any());
        }

        /**
         * CP8 | CE inválida: findById devuelve Optional.empty()
         * Mensaje exacto: "Pago no encontrado: " + pagoId
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
    // CE — Clases de Equivalencia  [procesarReembolso]
    // =========================================================================

    @Nested
    @DisplayName("CE procesarReembolso — Clases de Equivalencia")
    class CeProcesarReembolso {

        /**
         * CP9 | CE válida: reserva existe + pago existe + estado=COMPLETADO
         * → pago.reembolsar(): estado=REEMBOLSADO, fechaReembolso asignada
         */
        @Test
        @DisplayName("CP9 · CE válida · pago COMPLETADO → estado=REEMBOLSADO, fechaReembolso asignada")
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
         * Mensaje exacto: "Reserva no encontrada: " + reservaId
         */
        @Test
        @DisplayName("CP10 · CE inválida · C=T (reserva no existe) → IllegalArgumentException: Reserva no encontrada")
        void cp10_reservaNoExiste_illegalArgument() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");

            verify(pagoRepository, never()).findByReserva(any());
        }

        /**
         * CP11 | D=T — findByReserva devuelve Optional.empty()
         * Mensaje exacto: "No existe pago para esta reserva"
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
         * CP12 | E=T — pago.estado=PENDIENTE ≠ COMPLETADO
         * Mensaje exacto: "El pago no está en estado completado"
         */
        @Test
        @DisplayName("CP12 · CE inválida · E=T (estado=PENDIENTE) → IllegalStateException: no está en estado completado")
        void cp12_pagoPendiente_illegalState() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            // estado=PENDIENTE por defecto

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
         * VL-CP1 | importe del pago = reserva.getImporteTotal()
         * Verifica que Pago constructor asigna exactamente el importe de la reserva
         * sin modificarlo: 3 noches × 100€ → 336.0
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
         * VL-CP2 | estado COMPLETADA (= límite distinto de PENDIENTE_PAGO)
         * B=T con estado=COMPLETADA
         */
        @Test
        @DisplayName("VL-CP2 · reserva.estado=COMPLETADA → IllegalStateException: no está pendiente")
        void vlCp2_reservaCompletada_illegalState() {
            reservaPendiente.completar(); // estado → COMPLETADA
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /**
         * VL-CP3 | pago.estado=FALLIDO → E=T → IllegalStateException
         */
        @Test
        @DisplayName("VL-CP3 · pago.estado=FALLIDO → IllegalStateException: no está en estado completado")
        void vlCp3_pagoFallido_illegalState() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            pago.marcarFallido(); // estado → FALLIDO

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }

        /**
         * VL-CP4 | pago.estado=REEMBOLSADO → E=T → IllegalStateException
         * Un pago ya reembolsado no puede volver a reembolsarse
         */
        @Test
        @DisplayName("VL-CP4 · pago.estado=REEMBOLSADO → IllegalStateException: no está en estado completado")
        void vlCp4_pagoYaReembolsado_illegalState() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            pago.completar("TXN-ABCD1234");
            pago.reembolsar(); // estado → REEMBOLSADO

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }

        /**
         * VL-CP5 | confirmarPago genera referenciaExterna con formato "TXN-XXXXXXXX"
         * Verifica el formato: longitud exacta 12 chars y prefijo "TXN-"
         */
        @Test
        @DisplayName("VL-CP5 · referenciaExterna = 'TXN-' + 8 chars mayúsculas → longitud=12")
        void vlCp5_referenciaExternaFormato() {
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
    // DEC — Tabla de Decisión  [iniciarPago]
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

        /** DEC-CP2 | A=T → Dec1=T → Dec3=T */
        @Test
        @DisplayName("DEC-CP2 · A=T (solo) → Dec1=T → IllegalArgumentException")
        void decCp2_soloA() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /** DEC-CP3 | B=T → Dec2=T → Dec3=T (CONFIRMADA) */
        @Test
        @DisplayName("DEC-CP3 · B=T (CONFIRMADA) → Dec2=T → IllegalStateException")
        void decCp3_soloB_confirmada() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaConfirmada));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /** DEC-CP4 | B=T → Dec2=T (CANCELADA) */
        @Test
        @DisplayName("DEC-CP4 · B=T (CANCELADA) → Dec2=T → IllegalStateException")
        void decCp4_soloB_cancelada() {
            reservaPendiente.cancelar();
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /** DEC-CP5 | B=T → Dec2=T (COMPLETADA) */
        @Test
        @DisplayName("DEC-CP5 · B=T (COMPLETADA) → Dec2=T → IllegalStateException")
        void decCp5_soloB_completada() {
            reservaPendiente.completar();
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
            // B no puede evaluarse: A lanza antes de obtener la reserva

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
        @DisplayName("MCD-CP2 · caso base procesarReembolso (C=F,D=F,E=F) → Reembolso OK")
        void mcdCp2_casoBaseReembolso() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            pago.completar("TXN-12345678");

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(pagoService.procesarReembolso(1L).getEstado())
                .isEqualTo(Pago.EstadoPago.REEMBOLSADO);
        }

        /** MCD-CP3 | B=T (solo) → Dec2=T → B domina */
        @Test
        @DisplayName("MCD-CP3 · B=T solo → B domina Dec2 → IllegalStateException")
        void mcdCp3_bDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaConfirmada));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /** MCD-CP4 | A=T (solo) → Dec1=T → A domina */
        @Test
        @DisplayName("MCD-CP4 · A=T solo → A domina Dec1 → IllegalArgumentException")
        void mcdCp4_aDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /**
         * MCD-CP5 | A=T, B=T → A evalúa antes que B
         * findById vacío → A lanza antes de obtener la reserva
         */
        @Test
        @DisplayName("MCD-CP5 · A=T,B=T → A evalúa antes que B → IllegalArgumentException")
        void mcdCp5_aYb_aAntes() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /** MCD-CP6 | Caso base procesarReembolso confirmado nuevamente */
        @Test
        @DisplayName("MCD-CP6 · caso base procesarReembolso → Pago REEMBOLSADO")
        void mcdCp6_casoBaseReembolso2() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.PAYPAL, reservaPendiente);
            pago.completar("TXN-AAAAAAAA");

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));
            when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(pagoService.procesarReembolso(1L).getEstado())
                .isEqualTo(Pago.EstadoPago.REEMBOLSADO);
        }

        /** MCD-CP7 | C=T (solo) → Dec4=T → C domina en procesarReembolso */
        @Test
        @DisplayName("MCD-CP7 · C=T solo → Dec4=T → C domina → IllegalArgumentException")
        void mcdCp7_cDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /** MCD-CP8 | D=T (solo) → Dec5=T → D domina en procesarReembolso */
        @Test
        @DisplayName("MCD-CP8 · D=T solo → Dec5=T → D domina → IllegalStateException: No existe pago")
        void mcdCp8_dDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe pago para esta reserva");
        }

        /** MCD-CP9 | E=T (solo) → Dec6=T → E domina en procesarReembolso */
        @Test
        @DisplayName("MCD-CP9 · E=T solo → Dec6=T → E domina → IllegalStateException: no está en estado completado")
        void mcdCp9_eDomina() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_CREDITO, reservaPendiente);
            // estado=PENDIENTE por defecto → E=T

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }

        /** MCD-CP10 | C=T, D=T → C evalúa antes que D */
        @Test
        @DisplayName("MCD-CP10 · C=T,D=T → C evalúa antes que D → IllegalArgumentException")
        void mcdCp10_cYd_cAntes() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());
            // D no se evalúa: C lanza antes

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
            // E no se evalúa: D lanza antes

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe pago para esta reserva");
        }

        /** MCD-CP12 | C=T, D=T, E=T → C evalúa primero */
        @Test
        @DisplayName("MCD-CP12 · C=T,D=T,E=T → C evalúa primero → IllegalArgumentException")
        void mcdCp12_cYdYe_cPrimero() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }

        /** MCD-CP13 | B variante CANCELADA: B=T → Dec2=T */
        @Test
        @DisplayName("MCD-CP13 · B=T (CANCELADA) → Dec2=T → IllegalStateException")
        void mcdCp13_bCancelada() {
            reservaPendiente.cancelar();
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.PAYPAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /** MCD-CP14 | B variante COMPLETADA: B=T → Dec2=T */
        @Test
        @DisplayName("MCD-CP14 · B=T (COMPLETADA) → Dec2=T → IllegalStateException")
        void mcdCp14_bCompletada() {
            reservaPendiente.completar();
            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.PAYPAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva no está pendiente de pago");
        }

        /** MCD-CP15 | E variante FALLIDO: E=T → Dec6=T */
        @Test
        @DisplayName("MCD-CP15 · E=T (FALLIDO) → Dec6=T → IllegalStateException: no está en estado completado")
        void mcdCp15_eFallido() {
            Pago pago = new Pago(336.0, Pago.MetodoPago.TARJETA_DEBITO, reservaPendiente);
            pago.marcarFallido();

            when(reservaRepository.findById(any())).thenReturn(Optional.of(reservaPendiente));
            when(pagoRepository.findByReserva(any())).thenReturn(Optional.of(pago));

            assertThatThrownBy(() -> pagoService.procesarReembolso(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El pago no está en estado completado");
        }

        /** MCD-CP16 | A=T,B=T (ini) + C=T,D=T,E=T (rem) → A/C dominan */
        @Test
        @DisplayName("MCD-CP16 · todas T → A domina en iniciarPago → IllegalArgumentException")
        void mcdCp16_todasTrue_aDomina() {
            when(reservaRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                pagoService.iniciarPago(1L, Pago.MetodoPago.TARJETA_CREDITO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserva no encontrada");
        }
    }

    // =========================================================================
    // Método obtenerPagoPorReserva
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

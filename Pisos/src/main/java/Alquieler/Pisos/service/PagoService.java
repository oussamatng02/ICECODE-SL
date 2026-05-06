package Alquieler.Pisos.service;

import Alquieler.Pisos.entity.Pago;
import Alquieler.Pisos.entity.Reserva;
import Alquieler.Pisos.repository.PagoRepository;
import Alquieler.Pisos.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * GestorPagos: procesa pagos con tarjeta y PayPal.
 * En producción, aquí se integraría con Stripe, Redsys o PayPal SDK.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;

    /**
     * Inicia el proceso de pago para una reserva.
     */
    public Pago iniciarPago(Long reservaId, Pago.MetodoPago metodo) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada: " + reservaId));

        if (reserva.getEstado() != Reserva.EstadoReserva.PENDIENTE_PAGO) {
            throw new IllegalStateException("La reserva no está pendiente de pago");
        }

        Pago pago = new Pago(reserva.getImporteTotal(), metodo, reserva);
        return pagoRepository.save(pago);
    }

    /**
     * Simula la confirmación del pago por parte de la pasarela externa.
     * En producción: webhook de Stripe / callback de PayPal.
     */
    public Pago confirmarPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));

        // Simulamos referencia externa de la pasarela
        String referenciaExterna = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        pago.completar(referenciaExterna);
        pagoRepository.save(pago);

        // Confirmar la reserva asociada
        reservaService.confirmarTrasPago(pago.getReserva().getId());

        return pago;
    }

    /**
     * Procesa el reembolso según la política de cancelación del inmueble.
     */
    public Pago procesarReembolso(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada: " + reservaId));

        Pago pago = pagoRepository.findByReserva(reserva)
            .orElseThrow(() -> new IllegalStateException("No existe pago para esta reserva"));

        if (pago.getEstado() != Pago.EstadoPago.COMPLETADO) {
            throw new IllegalStateException("El pago no está en estado completado");
        }

        pago.reembolsar();
        return pagoRepository.save(pago);
    }

    @Transactional(readOnly = true)
    public Optional<Pago> obtenerPagoPorReserva(Reserva reserva) {
        return pagoRepository.findByReserva(reserva);
    }
}

package Alquieler.Pisos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que gestiona el pago de una reserva.
 * Métodos soportados: Tarjeta (crédito/débito) y PayPal.
 */
@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    public enum MetodoPago {
        TARJETA_CREDITO, TARJETA_DEBITO, PAYPAL
    }

    public enum EstadoPago {
        PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    /** Referencia externa de la pasarela de pago (ej. PayPal transaction ID) */
    @Column(length = 100)
    private String referenciaExterna;

    @Column(nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    private LocalDateTime fechaReembolso;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    public Pago(double importe, MetodoPago metodo, Reserva reserva) {
        this.importe = importe;
        this.metodo = metodo;
        this.reserva = reserva;
        this.fechaPago = LocalDateTime.now();
    }

    public void completar(String referenciaExterna) {
        this.estado = EstadoPago.COMPLETADO;
        this.referenciaExterna = referenciaExterna;
    }

    public void marcarFallido() {
        this.estado = EstadoPago.FALLIDO;
    }

    public void reembolsar() {
        this.estado = EstadoPago.REEMBOLSADO;
        this.fechaReembolso = LocalDateTime.now();
    }
}

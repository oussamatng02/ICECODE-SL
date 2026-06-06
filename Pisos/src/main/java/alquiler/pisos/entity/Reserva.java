package alquiler.pisos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    public enum EstadoReserva {
        PENDIENTE_PAGO, CONFIRMADA, CANCELADA, COMPLETADA
    }

    public enum TipoFlujo {
        INMEDIATA, SOLICITUD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaEntrada;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaSalida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.PENDIENTE_PAGO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFlujo tipoFlujo;

    @Column(nullable = false)
    private double importeTotal;

    @Column(nullable = false)
    private double tarifaServicio;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquilino_id", nullable = false)
    private Inquilino inquilino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private Inmueble inmueble;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Pago pago;

    @OneToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudReserva solicitudOrigen;

    public Reserva(LocalDate fechaEntrada, LocalDate fechaSalida,
                   Inquilino inquilino, Inmueble inmueble, TipoFlujo tipoFlujo) {
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.inquilino = inquilino;
        this.inmueble = inmueble;
        this.tipoFlujo = tipoFlujo;
        this.fechaCreacion = LocalDateTime.now();
        calcularImporte();
    }

    public long getNumNoches() {
        return ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    public void calcularImporte() {
        double base = inmueble.getPrecioPorNoche() * getNumNoches();
        this.tarifaServicio = Math.round(base * 0.12 * 100.0) / 100.0; // 12% tarifa
        this.importeTotal = Math.round((base + tarifaServicio) * 100.0) / 100.0;
    }

    public void confirmar() {
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public void completar() {
        this.estado = EstadoReserva.COMPLETADA;
    }
}

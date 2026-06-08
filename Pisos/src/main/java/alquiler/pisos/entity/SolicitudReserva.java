package alquiler.pisos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "solicitudes_reserva")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudReserva {

    public enum EstadoSolicitud {
        PENDIENTE, ACEPTADA, RECHAZADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaEntradaSolicitada;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaSalidaSolicitada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(length = 500)
    private String mensajeInquilino;

    @Column(length = 500)
    private String mensajePropietario;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaRespuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquilino_id", nullable = false)
    private Inquilino inquilino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private Inmueble inmueble;

    @OneToOne(mappedBy = "solicitudOrigen")
    private Reserva reservaGenerada;

    public SolicitudReserva(LocalDate entrada, LocalDate salida,
                             Inquilino inquilino, Inmueble inmueble, String mensaje) {
        this.fechaEntradaSolicitada = entrada;
        this.fechaSalidaSolicitada = salida;
        this.inquilino = inquilino;
        this.inmueble = inmueble;
        this.mensajeInquilino = mensaje;
        this.fechaSolicitud = LocalDateTime.now();
    }

    public void aceptar(String mensajePropietario) {
        this.estado = EstadoSolicitud.ACEPTADA;
        this.mensajePropietario = mensajePropietario;
        this.fechaRespuesta = LocalDateTime.now();
    }

    public void rechazar(String mensajePropietario) {
        this.estado = EstadoSolicitud.RECHAZADA;
        this.mensajePropietario = mensajePropietario;
        this.fechaRespuesta = LocalDateTime.now();
    }

    public boolean isPendiente() {
        return estado == EstadoSolicitud.PENDIENTE;
    }
}

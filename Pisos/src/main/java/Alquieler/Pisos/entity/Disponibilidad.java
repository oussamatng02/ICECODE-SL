package Alquieler.Pisos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Periodos de disponibilidad de un inmueble.
 * Se bloquea automáticamente al confirmar una reserva.
 */
@Entity
@Table(name = "disponibilidades")
@Getter
@Setter
@NoArgsConstructor
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaInicio;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean disponible = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private Inmueble inmueble;

    public Disponibilidad(LocalDate fechaInicio, LocalDate fechaFin, Inmueble inmueble) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.inmueble = inmueble;
    }

    /** Verifica si un rango de fechas se solapa con este periodo. */
    public boolean seSolapa(LocalDate entrada, LocalDate salida) {
        return !entrada.isAfter(this.fechaFin) && !salida.isBefore(this.fechaInicio);
    }

    public void bloquear() {
        this.disponible = false;
    }

    public void liberar() {
        this.disponible = true;
    }
}

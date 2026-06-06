package alquiler.pisos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


//Entidad que representa un inmueble ofertado por un propietario.
 
@Entity
@Table(name = "inmuebles")
@Getter
@Setter
@NoArgsConstructor
public class Inmueble {

    public enum TipoInmueble {
        VIVIENDA_COMPLETA, HABITACION_PRIVADA, HABITACION_COMPARTIDA
    }

    public enum TipoFlujoReserva {
        INMEDIATA, SOLICITUD
    }

    public enum PoliticaCancelacion {
        FLEXIBLE,    
        MODERADA,   
        ESTRICTA     
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String titulo;

    @Size(max = 2000)
    @Column(length = 2000)
    private String descripcion;

    @NotBlank(message = "La ubicación es obligatoria")
    @Column(nullable = false, length = 200)
    private String ubicacion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoInmueble tipo;

    @Positive(message = "El precio debe ser positivo")
    @Column(nullable = false)
    private double precioPorNoche;

    @Min(1) @Max(20)
    @Column(nullable = false)
    private int capacidadMaxima = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFlujoReserva tipoFlujo = TipoFlujoReserva.INMEDIATA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PoliticaCancelacion politicaCancelacion = PoliticaCancelacion.FLEXIBLE;

    // Comodidades como flags booleanos
    @Column(nullable = false)
    private boolean wifi = false;

    @Column(nullable = false)
    private boolean parking = false;

    @Column(nullable = false)
    private boolean airConditioning = false;

    @Column(nullable = false)
    private boolean cocina = false;

    @Column(nullable = false)
    private boolean lavadora = false;

    @Column(nullable = false)
    private boolean televisor = false;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private double valoracion = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Propietario propietario;

    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Disponibilidad> disponibilidades = new ArrayList<>();

    @OneToMany(mappedBy = "inmueble", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reserva> reservas = new ArrayList<>();

    public Inmueble(String titulo, String descripcion, String ubicacion,
                    TipoInmueble tipo, double precioPorNoche, Propietario propietario) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.tipo = tipo;
        this.precioPorNoche = precioPorNoche;
        this.propietario = propietario;
    }
}

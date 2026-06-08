package alquiler.pisos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


//Inquilino: usuario que busca y reserva inmuebles.

@Entity
@DiscriminatorValue("INQUILINO")
@Getter
@Setter
@NoArgsConstructor
public class Inquilino extends Usuario {

    @Column(nullable = false)
    private double valoracion = 0.0;

    @Column(nullable = false)
    private int numResenas = 0;

    //Lista de deseos: inmuebles guardados por el inquilino.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lista_deseos",
        joinColumns = @JoinColumn(name = "inquilino_id"),
        inverseJoinColumns = @JoinColumn(name = "inmueble_id")
    )
    private List<Inmueble> listaDeseos = new ArrayList<>();

    @OneToMany(mappedBy = "inquilino", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reserva> reservas = new ArrayList<>();

    public Inquilino(String nombre, String email, String contrasena) {
        super(nombre, email, contrasena);
    }

    public int getNumReservas() {
        return reservas.size();
    }

    public void agregarAListaDeseos(Inmueble inmueble) {
        if (!listaDeseos.contains(inmueble)) {
            listaDeseos.add(inmueble);
        }
    }

    public void eliminarDeListaDeseos(Inmueble inmueble) {
        listaDeseos.remove(inmueble);
    }

    public void actualizarValoracion(double nuevaValoracion) {
        this.valoracion = ((this.valoracion * this.numResenas) + nuevaValoracion) / (this.numResenas + 1);
        this.numResenas++;
    }
}

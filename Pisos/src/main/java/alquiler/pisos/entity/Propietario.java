package alquiler.pisos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


//Propietario: usuario que oferta inmuebles.

@Entity
@DiscriminatorValue("PROPIETARIO")
@Getter
@Setter
@NoArgsConstructor
public class Propietario extends Usuario {

    @Column(length = 34)
    private String iban;

    @Column(nullable = false)
    private double valoracion = 0.0;

    @Column(nullable = false)
    private int numResenas = 0;

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inmueble> inmuebles = new ArrayList<>();

    public Propietario(String nombre, String email, String contrasena) {
        super(nombre, email, contrasena);
    }

    public int getNumPropiedades() {
        return inmuebles.size();
    }

    // Actualiza la valoración media al recibir una nueva reseña.
    public void actualizarValoracion(double nuevaValoracion) {
        this.valoracion = ((this.valoracion * this.numResenas) + nuevaValoracion) / (this.numResenas + 1);
        this.numResenas++;
    }
}

package Alquieler.Pisos.repository;

import Alquieler.Pisos.entity.Disponibilidad;
import Alquieler.Pisos.entity.Inmueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    List<Disponibilidad> findByInmueble(Inmueble inmueble);

    List<Disponibilidad> findByInmuebleAndDisponibleTrue(Inmueble inmueble);

    List<Disponibilidad> findByInmuebleAndFechaInicioGreaterThanEqual(Inmueble inmueble, LocalDate desde);
}

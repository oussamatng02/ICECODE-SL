package alquiler.pisos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import alquiler.pisos.entity.Inmueble;
import alquiler.pisos.entity.Inquilino;
import alquiler.pisos.entity.SolicitudReserva;

import java.util.List;

@Repository
public interface SolicitudReservaRepository extends JpaRepository<SolicitudReserva, Long> {

    List<SolicitudReserva> findByInquilino(Inquilino inquilino);

    List<SolicitudReserva> findByInmueble(Inmueble inmueble);

    List<SolicitudReserva> findByEstado(SolicitudReserva.EstadoSolicitud estado);

    @Query("""
        SELECT s FROM SolicitudReserva s
        WHERE s.inmueble.propietario.id = :propietarioId
        ORDER BY s.fechaSolicitud DESC
        """)
    List<SolicitudReserva> findPendientesByPropietarioId(@Param("propietarioId") Long propietarioId);
}

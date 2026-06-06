package alquiler.pisos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import alquiler.pisos.entity.Inmueble;
import alquiler.pisos.entity.Inquilino;
import alquiler.pisos.entity.Reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO para Reserva.
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByInquilinoOrderByFechaCreacionDesc(Inquilino inquilino);

    List<Reserva> findByInmueble(Inmueble inmueble);

    List<Reserva> findByEstado(Reserva.EstadoReserva estado);

    /** Comprueba si hay solapamiento de fechas para un inmueble (para validación). */
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.inmueble = :inmueble
          AND r.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA')
          AND r.fechaEntrada < :salida
          AND r.fechaSalida > :entrada
        """)
    boolean existeSolapamiento(
        @Param("inmueble") Inmueble inmueble,
        @Param("entrada") LocalDate entrada,
        @Param("salida") LocalDate salida
    );

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.inmueble.propietario.id = :propietarioId
        ORDER BY r.fechaCreacion DESC
        """)
    List<Reserva> findByPropietarioId(@Param("propietarioId") Long propietarioId);
}

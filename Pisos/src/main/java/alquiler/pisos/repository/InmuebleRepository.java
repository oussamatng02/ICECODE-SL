package alquiler.pisos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import alquiler.pisos.entity.Inmueble;
import alquiler.pisos.entity.Propietario;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO para Inmueble.
 * Extiende JpaSpecificationExecutor para búsquedas dinámicas con filtros.
 */
@Repository
public interface InmuebleRepository extends JpaRepository<Inmueble, Long>,
        JpaSpecificationExecutor<Inmueble> {

    List<Inmueble> findByActivoTrue();

    List<Inmueble> findByPropietario(Propietario propietario);

    List<Inmueble> findByUbicacionContainingIgnoreCaseAndActivoTrue(String ubicacion);

    List<Inmueble> findByTipoAndActivoTrue(Inmueble.TipoInmueble tipo);

    List<Inmueble> findByTipoFlujoAndActivoTrue(Inmueble.TipoFlujoReserva tipoFlujo);

    /**
     * Busca inmuebles disponibles en un rango de fechas (sin reservas confirmadas solapadas).
     */
    @Query("""
        SELECT i FROM Inmueble i
        WHERE i.activo = true
          AND (:ubicacion IS NULL OR LOWER(i.ubicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%')))
          AND NOT EXISTS (
              SELECT r FROM Reserva r
              WHERE r.inmueble = i
                AND r.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA')
                AND r.fechaEntrada < :salida
                AND r.fechaSalida > :entrada
          )
        """)
    List<Inmueble> findDisponibles(
        @Param("ubicacion") String ubicacion,
        @Param("entrada") LocalDate entrada,
        @Param("salida") LocalDate salida
    );

    @Query("""
        SELECT i FROM Inmueble i
        WHERE i.activo = true
          AND (:ubicacion IS NULL OR LOWER(i.ubicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%')))
          AND (:tipo IS NULL OR i.tipo = :tipo)
          AND (:tipoFlujo IS NULL OR i.tipoFlujo = :tipoFlujo)
          AND (:precioMax IS NULL OR i.precioPorNoche <= :precioMax)
          AND (:wifi = false OR i.wifi = true)
          AND (:parking = false OR i.parking = true)
        """)
    List<Inmueble> buscarConFiltros(
        @Param("ubicacion") String ubicacion,
        @Param("tipo") Inmueble.TipoInmueble tipo,
        @Param("tipoFlujo") Inmueble.TipoFlujoReserva tipoFlujo,
        @Param("precioMax") Double precioMax,
        @Param("wifi") boolean wifi,
        @Param("parking") boolean parking
    );
}

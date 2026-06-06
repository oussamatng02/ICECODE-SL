package alquiler.pisos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import alquiler.pisos.entity.Pago;
import alquiler.pisos.entity.Reserva;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReserva(Reserva reserva);

    Optional<Pago> findByReferenciaExterna(String referencia);
}

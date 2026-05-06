package Alquieler.Pisos.repository;

import Alquieler.Pisos.entity.Pago;
import Alquieler.Pisos.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReserva(Reserva reserva);

    Optional<Pago> findByReferenciaExterna(String referencia);
}

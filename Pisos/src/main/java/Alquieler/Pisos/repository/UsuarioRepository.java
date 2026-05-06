package Alquieler.Pisos.repository;

import Alquieler.Pisos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


//DAO para Usuario. Spring Data JPA genera la implementación en tiempo de ejecución.
 
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}

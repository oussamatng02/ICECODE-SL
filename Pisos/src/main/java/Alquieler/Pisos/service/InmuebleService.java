package Alquieler.Pisos.service;

import Alquieler.Pisos.entity.Disponibilidad;
import Alquieler.Pisos.entity.Inmueble;
import Alquieler.Pisos.entity.Propietario;
import Alquieler.Pisos.repository.DisponibilidadRepository;
import Alquieler.Pisos.repository.InmuebleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * GestorInmuebles: alta de propiedades, gestión de disponibilidad y búsqueda.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InmuebleService {

    private final InmuebleRepository inmuebleRepository;
    private final DisponibilidadRepository disponibilidadRepository;

    public Inmueble darDeAltaInmueble(Inmueble inmueble) {
        return inmuebleRepository.save(inmueble);
    }

    public Inmueble actualizarInmueble(Inmueble inmueble) {
        return inmuebleRepository.save(inmueble);
    }

    public void desactivarInmueble(Long id, Propietario propietario) {
        Inmueble inmueble = obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado: " + id));
        if (!inmueble.getPropietario().getId().equals(propietario.getId())) {
            throw new SecurityException("No tienes permiso para desactivar este inmueble");
        }
        inmueble.setActivo(false);
        inmuebleRepository.save(inmueble);
    }

    @Transactional(readOnly = true)
    public Optional<Inmueble> obtenerPorId(Long id) {
        return inmuebleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Inmueble> listarTodos() {
        return inmuebleRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Inmueble> listarPorPropietario(Propietario propietario) {
        return inmuebleRepository.findByPropietario(propietario);
    }

    /**
     * Búsqueda principal con filtros avanzados.
     */
    @Transactional(readOnly = true)
    public List<Inmueble> buscarConFiltros(String ubicacion, Inmueble.TipoInmueble tipo,
            Inmueble.TipoFlujoReserva tipoFlujo, Double precioMax,
            boolean wifi, boolean parking) {
        return inmuebleRepository.buscarConFiltros(ubicacion, tipo, tipoFlujo, precioMax, wifi, parking);
    }

    /**
     * Búsqueda por destino y fechas (filtra reservas solapadas).
     */
    @Transactional(readOnly = true)
    public List<Inmueble> buscarDisponibles(String ubicacion, LocalDate entrada, LocalDate salida) {
        return inmuebleRepository.findDisponibles(ubicacion, entrada, salida);
    }

    public Disponibilidad agregarDisponibilidad(Long inmuebleId, LocalDate inicio, LocalDate fin) {
        Inmueble inmueble = obtenerPorId(inmuebleId)
            .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado: " + inmuebleId));
        Disponibilidad disp = new Disponibilidad(inicio, fin, inmueble);
        return disponibilidadRepository.save(disp);
    }
}

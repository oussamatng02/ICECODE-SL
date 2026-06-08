package alquiler.pisos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import alquiler.pisos.entity.Inmueble;
import alquiler.pisos.entity.Inquilino;
import alquiler.pisos.repository.InmuebleRepository;
import alquiler.pisos.repository.UsuarioRepository;

import java.util.List;

/**
 * Gestiona la lista de deseos del inquilino.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ListaDeseosService {

    private final UsuarioRepository usuarioRepository;
    private final InmuebleRepository inmuebleRepository;

    public void agregar(Inquilino inquilino, Long inmuebleId) {
        Inmueble inmueble = inmuebleRepository.findById(inmuebleId)
            .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado: " + inmuebleId));
        inquilino.agregarAListaDeseos(inmueble);
        usuarioRepository.save(inquilino);
    }

    public void eliminar(Inquilino inquilino, Long inmuebleId) {
        Inmueble inmueble = inmuebleRepository.findById(inmuebleId)
            .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado: " + inmuebleId));
        inquilino.eliminarDeListaDeseos(inmueble);
        usuarioRepository.save(inquilino);
    }

    @Transactional(readOnly = true)
    public List<Inmueble> obtenerLista(Inquilino inquilino) {
        return inquilino.getListaDeseos();
    }

    @Transactional(readOnly = true)
    public boolean estaEnLista(Inquilino inquilino, Long inmuebleId) {
        return inquilino.getListaDeseos().stream()
            .anyMatch(i -> i.getId().equals(inmuebleId));
    }
}

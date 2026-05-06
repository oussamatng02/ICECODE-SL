package Alquieler.Pisos.service;

import Alquieler.Pisos.entity.Inquilino;
import Alquieler.Pisos.entity.Propietario;
import Alquieler.Pisos.entity.Usuario;
import Alquieler.Pisos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * GestorUsuarios: lógica de negocio para registro, autenticación y perfiles.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo propietario.
     * @throws IllegalArgumentException si el email ya existe.
     */
    public Propietario registrarPropietario(String nombre, String email, String contrasena) {
        validarEmailUnico(email);
        Propietario propietario = new Propietario(nombre, email, passwordEncoder.encode(contrasena));
        return usuarioRepository.save(propietario);
    }

    /**
     * Registra un nuevo inquilino.
     * @throws IllegalArgumentException si el email ya existe.
     */
    public Inquilino registrarInquilino(String nombre, String email, String contrasena) {
        validarEmailUnico(email);
        Inquilino inquilino = new Inquilino(nombre, email, passwordEncoder.encode(contrasena));
        return usuarioRepository.save(inquilino);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario actualizarPerfil(Long id, String nombre) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        usuario.setNombre(nombre);
        return usuarioRepository.save(usuario);
    }

    public void cambiarContrasena(Long id, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);
    }

    public void desactivarCuenta(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado: " + email);
        }
    }
}

package Alquieler.Pisos.config;

import Alquieler.Pisos.entity.Inquilino;
import Alquieler.Pisos.entity.Propietario;
import Alquieler.Pisos.entity.Usuario;
import Alquieler.Pisos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService para Spring Security.
 * Carga el usuario por email y asigna el rol según el tipo.
 */
@Service
@RequiredArgsConstructor
public class AlquilaYaUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("Cuenta desactivada: " + email);
        }

        String rol = (usuario instanceof Propietario) ? "ROLE_PROPIETARIO" : "ROLE_INQUILINO";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(rol));

        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getContrasena())
            .authorities(authorities)
            .build();
    }
}

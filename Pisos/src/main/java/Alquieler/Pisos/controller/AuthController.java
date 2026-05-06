package Alquieler.Pisos.controller;

import Alquieler.Pisos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @GetMapping("/login")
    public String loginForm(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null) model.addAttribute("errorMsg", "Email o contraseña incorrectos.");
        if (logout != null) model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("tiposUsuario", new String[]{"INQUILINO", "PROPIETARIO"});
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String contrasena,
            @RequestParam String confirmarContrasena,
            @RequestParam String tipoUsuario,
            RedirectAttributes ra) {

        if (!contrasena.equals(confirmarContrasena)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/auth/registro";
        }

        try {
            if ("PROPIETARIO".equals(tipoUsuario)) {
                usuarioService.registrarPropietario(nombre, email, contrasena);
            } else {
                usuarioService.registrarInquilino(nombre, email, contrasena);
            }
            ra.addFlashAttribute("exito", "Cuenta creada correctamente. Ya puedes iniciar sesión.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/registro";
        }
    }
}

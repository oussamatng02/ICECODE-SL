package Alquieler.Pisos.controller;

import Alquieler.Pisos.entity.Propietario;
import Alquieler.Pisos.service.InmuebleService;
import Alquieler.Pisos.service.ReservaService;
import Alquieler.Pisos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/propietario")
@RequiredArgsConstructor
public class PropietarioController {

    private final UsuarioService usuarioService;
    private final InmuebleService inmuebleService;
    private final ReservaService reservaService;

  
    @GetMapping("/panel")
    public String panel(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Propietario propietario = obtenerPropietario(userDetails);
        model.addAttribute("propietario", propietario);
        model.addAttribute("inmuebles", inmuebleService.listarPorPropietario(propietario));
        model.addAttribute("solicitudesPendientes",
            reservaService.listarSolicitudesPendientes(propietario.getId()));
        model.addAttribute("reservasRecientes",
            reservaService.listarPorPropietario(propietario.getId()));
        return "propietario/panel";
    }

    // Acepta una solicitud de reserva.
    @PostMapping("/solicitudes/{id}/aceptar")
    public String aceptarSolicitud(@PathVariable Long id,
                                    @RequestParam(required = false) String mensaje,
                                    RedirectAttributes ra) {
        try {
            reservaService.aceptarSolicitud(id, mensaje);
            ra.addFlashAttribute("exito", "Solicitud aceptada. El inquilino será notificado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/panel";
    }

    // Rechaza una solicitud de reserva.
    @PostMapping("/solicitudes/{id}/rechazar")
    public String rechazarSolicitud(@PathVariable Long id,
                                     @RequestParam(required = false) String mensaje,
                                     RedirectAttributes ra) {
        try {
            reservaService.rechazarSolicitud(id, mensaje);
            ra.addFlashAttribute("exito", "Solicitud rechazada. El inquilino será notificado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/panel";
    }

    // Desactiva un inmueble del propietario.
    @PostMapping("/inmuebles/{id}/desactivar")
    public String desactivarInmueble(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes ra) {
        Propietario propietario = obtenerPropietario(userDetails);
        try {
            inmuebleService.desactivarInmueble(id, propietario);
            ra.addFlashAttribute("exito", "Inmueble desactivado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/panel";
    }

    private Propietario obtenerPropietario(UserDetails userDetails) {
        return (Propietario) usuarioService
            .buscarPorEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("Propietario no encontrado"));
    }
}

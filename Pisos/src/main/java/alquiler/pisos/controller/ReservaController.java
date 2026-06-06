package alquiler.pisos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import alquiler.pisos.entity.*;
import alquiler.pisos.service.ReservaService;
import alquiler.pisos.service.UsuarioService;

import java.time.LocalDate;


@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    private String exito="exito";
    private String error="error";

    @GetMapping("/mis-reservas")
    public String misReservas(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Inquilino inquilino = obtenerInquilino(userDetails);
        model.addAttribute("reservas", reservaService.listarPorInquilino(inquilino));
        return "reserva/mis-reservas";
    }

    @GetMapping("/{id}")
    public String detalleReserva(@PathVariable Long id, Model model) {
        Reserva reserva = reservaService.obtenerReserva(id)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        model.addAttribute("reserva", reserva);
        return "reserva/detalle";
    }

    @GetMapping("/nueva/{inmuebleId}")
    public String formularioReserva(@PathVariable Long inmuebleId,
                                     @RequestParam(required = false) String entrada,
                                     @RequestParam(required = false) String salida,
                                     Model model) {
        model.addAttribute("inmuebleId", inmuebleId);
        model.addAttribute("entrada", entrada);
        model.addAttribute("salida", salida);
        return "reserva/nueva-reserva";
    }

    @PostMapping("/nueva/inmediata")
    public String crearReservaInmediata(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long inmuebleId,
            @RequestParam String fechaEntrada,
            @RequestParam String fechaSalida,
            RedirectAttributes ra) {

        try {
            Inquilino inquilino = obtenerInquilino(userDetails);
            Reserva reserva = reservaService.crearReservaInmediata(
                inmuebleId, inquilino,
                LocalDate.parse(fechaEntrada),
                LocalDate.parse(fechaSalida)
            );
            return "redirect:/pagos/procesar/" + reserva.getId();
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute(error, e.getMessage());
            return "redirect:/inmuebles/" + inmuebleId;
        }
    }

    @PostMapping("/nueva/solicitud")
    public String crearSolicitud(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long inmuebleId,
            @RequestParam String fechaEntrada,
            @RequestParam String fechaSalida,
            @RequestParam(required = false) String mensaje,
            RedirectAttributes ra) {

        try {
            Inquilino inquilino = obtenerInquilino(userDetails);
            reservaService.crearSolicitud(
                inmuebleId, inquilino,
                LocalDate.parse(fechaEntrada),
                LocalDate.parse(fechaSalida),
                mensaje
            );
            ra.addFlashAttribute(exito,
                "Solicitud enviada. El propietario revisará tu petición y recibirás una notificación.");
            return "redirect:/reservas/mis-reservas";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute(error, e.getMessage());
            return "redirect:/inmuebles/" + inmuebleId;
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelarReserva(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes ra) {
        try {
            Usuario usuario = usuarioService.buscarPorEmail(userDetails.getUsername()).orElseThrow();
            reservaService.cancelarReserva(id, usuario.getId());
            ra.addFlashAttribute(exito, "Reserva cancelada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute(error, e.getMessage());
        }
        return "redirect:/reservas/mis-reservas";
    }


    @GetMapping("/{id}/confirmacion")
    public String paginaConfirmacion(@PathVariable Long id, Model model) {
        Reserva reserva = reservaService.obtenerReserva(id)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        model.addAttribute("reserva", reserva);
        return "reserva/confirmacion";
    }


    private Inquilino obtenerInquilino(UserDetails userDetails) {
        return (Inquilino) usuarioService
            .buscarPorEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }
}

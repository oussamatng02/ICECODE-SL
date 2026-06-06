package alquiler.pisos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import alquiler.pisos.entity.Pago;
import alquiler.pisos.entity.Reserva;
import alquiler.pisos.service.PagoService;
import alquiler.pisos.service.ReservaService;


@Controller
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final ReservaService reservaService;

    // Formulario de pago para una reserva pendiente.
    @GetMapping("/procesar/{reservaId}")
    public String formularioPago(@PathVariable Long reservaId, Model model) {
        Reserva reserva = reservaService.obtenerReserva(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        model.addAttribute("reserva", reserva);
        model.addAttribute("metodos", Pago.MetodoPago.values());
        return "reserva/pago";
    }

    // Procesa el pago: crea el registro y simula confirmación de pasarela.
    @PostMapping("/procesar/{reservaId}")
    public String procesarPago(
            @PathVariable Long reservaId,
            @RequestParam Pago.MetodoPago metodoPago,
            RedirectAttributes ra) {
        try {
            Pago pago = pagoService.iniciarPago(reservaId, metodoPago);
            // Simular confirmación inmediata de la pasarela (en producción: webhook)
            pagoService.confirmarPago(pago.getId());
            ra.addFlashAttribute("exito", "¡Pago completado! Tu reserva está confirmada.");
            return "redirect:/reservas/" + reservaId + "/confirmacion";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al procesar el pago: " + e.getMessage());
            return "redirect:/pagos/procesar/" + reservaId;
        }
    }

    // Página de confirmación tras pago exitoso.
    @GetMapping("/{reservaId}/confirmacion")
    public String confirmacion(@PathVariable Long reservaId, Model model) {
        // redirigir desde /reservas/{id}/confirmacion
        return "redirect:/reservas/" + reservaId + "/confirmacion";
    }
}

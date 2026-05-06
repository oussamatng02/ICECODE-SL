package Alquieler.Pisos.controller;

import Alquieler.Pisos.entity.Inquilino;
import Alquieler.Pisos.service.ListaDeseosService;
import Alquieler.Pisos.service.ReservaService;
import Alquieler.Pisos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Panel del inquilino: lista de deseos y perfil.
 */
@Controller
@RequestMapping("/inquilino")
@RequiredArgsConstructor
public class InquilinoController {

    private final UsuarioService usuarioService;
    private final ListaDeseosService listaDeseosService;
    private final ReservaService reservaService;

    @GetMapping("/panel")
    public String panel(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Inquilino inquilino = obtenerInquilino(userDetails);
        model.addAttribute("inquilino", inquilino);
        model.addAttribute("reservas", reservaService.listarPorInquilino(inquilino));
        model.addAttribute("listaDeseos", listaDeseosService.obtenerLista(inquilino));
        return "inquilino/panel";
    }

    @GetMapping("/lista-deseos")
    public String listaDeseos(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Inquilino inquilino = obtenerInquilino(userDetails);
        model.addAttribute("inmuebles", listaDeseosService.obtenerLista(inquilino));
        return "inquilino/lista-deseos";
    }

    @PostMapping("/lista-deseos/agregar/{inmuebleId}")
    public String agregarDeseo(@PathVariable Long inmuebleId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        Inquilino inquilino = obtenerInquilino(userDetails);
        listaDeseosService.agregar(inquilino, inmuebleId);
        ra.addFlashAttribute("exito", "Inmueble guardado en tu lista de deseos.");
        return "redirect:/inmuebles/" + inmuebleId;
    }

    @PostMapping("/lista-deseos/eliminar/{inmuebleId}")
    public String eliminarDeseo(@PathVariable Long inmuebleId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes ra) {
        Inquilino inquilino = obtenerInquilino(userDetails);
        listaDeseosService.eliminar(inquilino, inmuebleId);
        ra.addFlashAttribute("exito", "Inmueble eliminado de tu lista de deseos.");
        return "redirect:/inquilino/lista-deseos";
    }

    private Inquilino obtenerInquilino(UserDetails userDetails) {
        return (Inquilino) usuarioService
            .buscarPorEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("Inquilino no encontrado"));
    }
}

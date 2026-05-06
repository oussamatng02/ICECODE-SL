package Alquieler.Pisos.controller;

import Alquieler.Pisos.entity.Inmueble;
import Alquieler.Pisos.entity.Inquilino;
import Alquieler.Pisos.entity.Propietario;
import Alquieler.Pisos.service.InmuebleService;
import Alquieler.Pisos.service.ListaDeseosService;
import Alquieler.Pisos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * GestorInmuebles + GestorBúsqueda: búsqueda pública, detalle y alta de propiedades.
 */
@Controller
@RequiredArgsConstructor
public class InmuebleController {

    private final InmuebleService inmuebleService;
    private final UsuarioService usuarioService;
    private final ListaDeseosService listaDeseosService;

    /** Página principal: listado de inmuebles disponibles. */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("inmuebles", inmuebleService.listarTodos());
        return "inmueble/listado";
    }

    /** Búsqueda con filtros avanzados. */
    @GetMapping("/inmuebles/buscar")
    public String buscar(
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String entradaStr,
            @RequestParam(required = false) String salidaStr,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String tipoFlujo,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(defaultValue = "false") boolean wifi,
            @RequestParam(defaultValue = "false") boolean parking,
            Model model) {

        List<Inmueble> resultados;

        LocalDate entrada = (entradaStr != null && !entradaStr.isBlank())
                ? LocalDate.parse(entradaStr) : null;
        LocalDate salida = (salidaStr != null && !salidaStr.isBlank())
                ? LocalDate.parse(salidaStr) : null;

        if (entrada != null && salida != null && ubicacion != null) {
            resultados = inmuebleService.buscarDisponibles(ubicacion, entrada, salida);
        } else {
            Inmueble.TipoInmueble tipoEnum = (tipo != null && !tipo.isBlank())
                    ? Inmueble.TipoInmueble.valueOf(tipo) : null;
            Inmueble.TipoFlujoReserva flujoEnum = (tipoFlujo != null && !tipoFlujo.isBlank())
                    ? Inmueble.TipoFlujoReserva.valueOf(tipoFlujo) : null;
            resultados = inmuebleService.buscarConFiltros(ubicacion, tipoEnum, flujoEnum, precioMax, wifi, parking);
        }

        model.addAttribute("inmuebles", resultados);
        model.addAttribute("ubicacion", ubicacion);
        model.addAttribute("entrada", entradaStr);
        model.addAttribute("salida", salidaStr);
        model.addAttribute("tiposInmueble", Inmueble.TipoInmueble.values());
        model.addAttribute("tiposFlujo", Inmueble.TipoFlujoReserva.values());
        return "inmueble/buscar";
    }

    /** Detalle de un inmueble. */
    @GetMapping("/inmuebles/{id}")
    public String detalle(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        Inmueble inmueble = inmuebleService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado"));

        model.addAttribute("inmueble", inmueble);

        if (userDetails != null) {
            usuarioService.buscarPorEmail(userDetails.getUsername()).ifPresent(u -> {
                if (u instanceof Inquilino inquilino) {
                    model.addAttribute("enListaDeseos",
                        listaDeseosService.estaEnLista(inquilino, id));
                }
            });
        }
        return "inmueble/detalle";
    }

    // ─── PROPIETARIO: ALTA Y GESTIÓN ──────────────────────────────────────

    @GetMapping("/propietario/inmuebles/nuevo")
    public String nuevoInmuebleForm(Model model) {
        model.addAttribute("inmueble", new Inmueble());
        model.addAttribute("tipos", Inmueble.TipoInmueble.values());
        model.addAttribute("flujos", Inmueble.TipoFlujoReserva.values());
        model.addAttribute("politicas", Inmueble.PoliticaCancelacion.values());
        return "propietario/nuevo-inmueble";
    }

    @PostMapping("/propietario/inmuebles/nuevo")
    public String crearInmueble(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String titulo,
            @RequestParam String descripcion,
            @RequestParam String ubicacion,
            @RequestParam Inmueble.TipoInmueble tipo,
            @RequestParam double precioPorNoche,
            @RequestParam int capacidadMaxima,
            @RequestParam Inmueble.TipoFlujoReserva tipoFlujo,
            @RequestParam Inmueble.PoliticaCancelacion politicaCancelacion,
            @RequestParam(defaultValue = "false") boolean wifi,
            @RequestParam(defaultValue = "false") boolean parking,
            @RequestParam(defaultValue = "false") boolean airConditioning,
            @RequestParam(defaultValue = "false") boolean cocina,
            RedirectAttributes ra) {

        Propietario propietario = (Propietario) usuarioService
            .buscarPorEmail(userDetails.getUsername())
            .orElseThrow();

        Inmueble inmueble = new Inmueble(titulo, descripcion, ubicacion, tipo, precioPorNoche, propietario);
        inmueble.setCapacidadMaxima(capacidadMaxima);
        inmueble.setTipoFlujo(tipoFlujo);
        inmueble.setPoliticaCancelacion(politicaCancelacion);
        inmueble.setWifi(wifi);
        inmueble.setParking(parking);
        inmueble.setAirConditioning(airConditioning);
        inmueble.setCocina(cocina);

        inmuebleService.darDeAltaInmueble(inmueble);
        ra.addFlashAttribute("exito", "Propiedad publicada correctamente.");
        return "redirect:/propietario/panel";
    }
}

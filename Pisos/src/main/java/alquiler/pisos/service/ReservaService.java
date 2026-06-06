package alquiler.pisos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import alquiler.pisos.entity.*;
import alquiler.pisos.repository.InmuebleRepository;
import alquiler.pisos.repository.ReservaRepository;
import alquiler.pisos.repository.SolicitudReservaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SolicitudReservaRepository solicitudRepository;
    private final InmuebleRepository inmuebleRepository;
    private final NotificacionService notificacionService;


    /**
     * Crea una reserva inmediata (pago directo sin confirmación del propietario).
     */
    public Reserva crearReservaInmediata(Long inmuebleId, Inquilino inquilino,
                                          LocalDate entrada, LocalDate salida) {
        Inmueble inmueble = obtenerInmuebleOFallar(inmuebleId);
        validarFlujoInmediato(inmueble);
        validarFechas(entrada, salida);
        validarDisponibilidad(inmueble, entrada, salida);

        Reserva reserva = new Reserva(entrada, salida, inquilino, inmueble, Reserva.TipoFlujo.INMEDIATA);
        reserva = reservaRepository.save(reserva);

        notificacionService.notificarNuevaReserva(reserva);
        return reserva;
    }

//Confirma la reserva tras completar el pago.
    
    public Reserva confirmarTrasPago(Long reservaId) {
        Reserva reserva = obtenerReservaOFallar(reservaId);
        reserva.confirmar();
        reserva = reservaRepository.save(reserva);
        notificacionService.notificarReservaConfirmada(reserva);
        return reserva;
    }



    public SolicitudReserva crearSolicitud(Long inmuebleId, Inquilino inquilino,
                                            LocalDate entrada, LocalDate salida,
                                            String mensaje) {
        Inmueble inmueble = obtenerInmuebleOFallar(inmuebleId);
        validarFechas(entrada, salida);
        validarDisponibilidad(inmueble, entrada, salida);

        SolicitudReserva solicitud = new SolicitudReserva(entrada, salida, inquilino, inmueble, mensaje);
        solicitud = solicitudRepository.save(solicitud);

        notificacionService.notificarNuevaSolicitud(solicitud);
        return solicitud;
    }

    
   // El propietario acepta la solicitud → se genera una Reserva confirmada.
    
    public Reserva aceptarSolicitud(Long solicitudId, String mensajePropietario) {
        SolicitudReserva solicitud = obtenerSolicitudOFallar(solicitudId);
        if (!solicitud.isPendiente()) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        validarDisponibilidad(solicitud.getInmueble(),
                solicitud.getFechaEntradaSolicitada(),
                solicitud.getFechaSalidaSolicitada());

        solicitud.aceptar(mensajePropietario);
        solicitudRepository.save(solicitud);

        Reserva reserva = new Reserva(
                solicitud.getFechaEntradaSolicitada(),
                solicitud.getFechaSalidaSolicitada(),
                solicitud.getInquilino(),
                solicitud.getInmueble(),
                Reserva.TipoFlujo.SOLICITUD
        );
        reserva.setSolicitudOrigen(solicitud);
        reserva.confirmar(); // ya confirmada por el propietario
        reserva = reservaRepository.save(reserva);

        notificacionService.notificarSolicitudAceptada(solicitud);
        return reserva;
    }

    
     //El propietario rechaza la solicitud.
    
    public SolicitudReserva rechazarSolicitud(Long solicitudId, String mensajePropietario) {
        SolicitudReserva solicitud = obtenerSolicitudOFallar(solicitudId);
        if (!solicitud.isPendiente()) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        solicitud.rechazar(mensajePropietario);
        solicitud = solicitudRepository.save(solicitud);
        notificacionService.notificarSolicitudRechazada(solicitud);
        return solicitud;
    }

    // CANCELACIÓN 

    public Reserva cancelarReserva(Long reservaId, Long usuarioId) {
        Reserva reserva = obtenerReservaOFallar(reservaId);
        boolean esInquilino = reserva.getInquilino().getId().equals(usuarioId);
        boolean esPropietario = reserva.getInmueble().getPropietario().getId().equals(usuarioId);
        if (!esInquilino && !esPropietario) {
            throw new SecurityException("No tienes permiso para cancelar esta reserva");
        }
        reserva.cancelar();
        reserva = reservaRepository.save(reserva);
        notificacionService.notificarReservaCancelada(reserva);
        return reserva;
    }

    //  CONSULTAS

    @Transactional(readOnly = true)
    public List<Reserva> listarPorInquilino(Inquilino inquilino) {
        return reservaRepository.findByInquilinoOrderByFechaCreacionDesc(inquilino);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorPropietario(Long propietarioId) {
        return reservaRepository.findByPropietarioId(propietarioId);
    }

    @Transactional(readOnly = true)
    public List<SolicitudReserva> listarSolicitudesPendientes(Long propietarioId) {
        return solicitudRepository.findPendientesByPropietarioId(propietarioId);
    }

    @Transactional(readOnly = true)
    public Optional<Reserva> obtenerReserva(Long id) {
        return reservaRepository.findById(id);
    }

 

    private Inmueble obtenerInmuebleOFallar(Long id) {
        return inmuebleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inmueble no encontrado: " + id));
    }

    private Reserva obtenerReservaOFallar(Long id) {
        return reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada: " + id));
    }

    private SolicitudReserva obtenerSolicitudOFallar(Long id) {
        return solicitudRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + id));
    }

    private void validarFlujoInmediato(Inmueble inmueble) {
        if (inmueble.getTipoFlujo() != Inmueble.TipoFlujoReserva.INMEDIATA) {
            throw new IllegalStateException("Este inmueble requiere solicitud al propietario");
        }
    }

    private void validarFechas(LocalDate entrada, LocalDate salida) {
        if (!entrada.isBefore(salida)) {
            throw new IllegalArgumentException("La fecha de entrada debe ser anterior a la de salida");
        }
        if (entrada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser en el pasado");
        }
    }

    private void validarDisponibilidad(Inmueble inmueble, LocalDate entrada, LocalDate salida) {
        boolean solapado = reservaRepository.existeSolapamiento(inmueble, entrada, salida);
        if (solapado) {
            throw new IllegalStateException("El inmueble no está disponible en las fechas seleccionadas");
        }
    }
}

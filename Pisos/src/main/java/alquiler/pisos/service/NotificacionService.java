package alquiler.pisos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import alquiler.pisos.entity.Reserva;
import alquiler.pisos.entity.SolicitudReserva;

/**
 * GestorNotificaciones: envía notificaciones en eventos clave del sistema.
 * Implementación actual: logging. En producción: JavaMailSender / FCM push.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    public void notificarNuevaReserva(Reserva reserva) {
        log.info("[NOTIFICACION] Nueva reserva #{} para inmueble '{}'. Inquilino: {}",
            reserva.getId(),
            reserva.getInmueble().getTitulo(),
            reserva.getInquilino().getEmail());
        
    }

    public void notificarReservaConfirmada(Reserva reserva) {
        log.info("[NOTIFICACION] Reserva #{} CONFIRMADA. Email a: {}",
            reserva.getId(),
            reserva.getInquilino().getEmail());
        
    }

    public void notificarNuevaSolicitud(SolicitudReserva solicitud) {
        log.info("[NOTIFICACION] Nueva solicitud de reserva de {} para inmueble '{}'. Propietario: {}",
            solicitud.getInquilino().getEmail(),
            solicitud.getInmueble().getTitulo(),
            solicitud.getInmueble().getPropietario().getEmail());
        
    }

    public void notificarSolicitudAceptada(SolicitudReserva solicitud) {
        log.info("[NOTIFICACION] Solicitud #{} ACEPTADA. Email a: {}",
            solicitud.getId(),
            solicitud.getInquilino().getEmail());
        
    }

    public void notificarSolicitudRechazada(SolicitudReserva solicitud) {
        log.info("[NOTIFICACION] Solicitud #{} RECHAZADA. Email a: {}",
            solicitud.getId(),
            solicitud.getInquilino().getEmail());
        
    }

    public void notificarReservaCancelada(Reserva reserva) {
        log.info("[NOTIFICACION] Reserva #{} CANCELADA.",
            reserva.getId());
        
    }
}

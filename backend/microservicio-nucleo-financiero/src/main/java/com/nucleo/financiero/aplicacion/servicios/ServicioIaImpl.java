package com.nucleo.financiero.aplicacion.servicios;

import com.libreria.comun.dtos.ContextoUsuarioDTO;
import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import com.libreria.comun.enums.TipoSolicitudIa;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import java.util.UUID;
import com.nucleo.financiero.aplicacion.puertos.IServicioIa;
import com.nucleo.financiero.infraestructura.clientes.ClienteIa;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import com.nucleo.financiero.infraestructura.clientes.ClienteContexto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ImplementaciÃ³n de {@link IServicioIa} para la interacciÃ³n con el ecosistema de Inteligencia Artificial.
 * <p>
 * Coordina la recuperaciÃ³n de contexto del cliente, el enriquecimiento de solicitudes
 * y la comunicaciÃ³n con el motor de IA basado en Python. AdemÃ¡s, registra la actividad
 * analÃ­tica en el sistema de auditorÃ­a.
 * </p>
 *
 * @version 1.3.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioIaImpl implements IServicioIa {

    private final ClienteIa clienteIa;
    private final ClienteContexto clienteContexto;
    private final PublicadorAuditoria publicadorAuditoria;

    @Override
    public RespuestaIaDTO obtenerConsejoIA(SolicitudIaDTO solicitud, String ipCliente) {
        UUID tokenUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!tokenUsuarioId.equals(solicitud.getUsuarioId())) {
            throw new ExcepcionAccesoDenegado();
        }

        log.info("Iniciando proceso de IA para el usuario: {} desde IP: {}", solicitud.getUsuarioId(), ipCliente);

        // 1. Obtener contexto completo del cliente (Datos personales, perfil, metas, lÃ­mites)
        ContextoUsuarioDTO contextoEnriquecido = clienteContexto.obtenerContexto(solicitud.getUsuarioId());

        // 2. Re-construir la solicitud enriquecida con el contexto recuperado
        SolicitudIaDTO solicitudFinal;

        if (solicitud.getModuloSolicitado() != null) {
            solicitudFinal = SolicitudIaDTO.builder()
                    .usuarioId(solicitud.getUsuarioId())
                    .tipoSolicitud(TipoSolicitudIa.CONSULTA_MODULO)
                    .moduloSolicitado(solicitud.getModuloSolicitado())
                    .historialMensual(solicitud.getHistorialMensual())
                    .contexto(contextoEnriquecido)
                    .build();
        } else {
            solicitudFinal = SolicitudIaDTO.builder()
                    .usuarioId(solicitud.getUsuarioId())
                    .tipoSolicitud(TipoSolicitudIa.TRANSACCION_RECIENTE)
                    .historialMensual(solicitud.getHistorialMensual())
                    .contexto(contextoEnriquecido)
                    .build();
        }

        // 3. Llamada sÃ­ncrona al microservicio de IA (Python - FastAPI) vÃ­a Feign
        log.debug("Enviando solicitud enriquecida a Python para anÃ¡lisis...");
        RespuestaIaDTO respuesta = clienteIa.analizarFinanzas(solicitudFinal);

        // 4. Registro AsÃ­ncrono en AuditorÃ­a vÃ­a RabbitMQ
        publicadorAuditoria.publicarAcceso(
                solicitudFinal.getUsuarioId(),
                "CONSULTA_IA",
                "AnÃ¡lisis generado con contexto: " + (solicitudFinal.getModuloSolicitado() != null
                ? solicitudFinal.getModuloSolicitado() : "TRANSACCION_RECIENTE"),
                ipCliente
        );

        return respuesta;
    }
}

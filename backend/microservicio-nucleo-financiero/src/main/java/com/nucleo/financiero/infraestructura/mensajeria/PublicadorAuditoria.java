package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publicador de eventos de auditorÃ­a para el NÃºcleo Financiero.
 * <p>
 * Extiende de {@link PublicadorEventosBase} para heredar la lÃ³gica de envÃ­o
 * asÃ­ncrona centralizada.
 * Utiliza los DTOs oficiales de la librerÃ­a comÃºn para asegurar la
 * compatibilidad con el ms-auditoria.
 * </p>
 *
 * @version 1.2.1
 */
@Component
public class PublicadorAuditoria extends PublicadorEventosBase {

        public PublicadorAuditoria(RabbitTemplate rabbitTemplate) {
                super(rabbitTemplate);
        }

        /**
         * Publica un registro de auditorÃ­a transaccional para trazabilidad de cambios.
         * 
         * @param usuarioId  ID del usuario que realiza la acciÃ³n.
         * @param entidadId  ID de la entidad afectada (TransacciÃ³n, CategorÃ­a).
         * @param valorNuevo RepresentaciÃ³n JSON o String del nuevo estado del objeto.
         * @param ip         DirecciÃ³n IP desde donde se realiza la acciÃ³n.
         */
        public void publicarRegistro(UUID usuarioId, UUID entidadId, String valorNuevo, String ip) {
                // Mapeo al DTO oficial de la librerÃ­a
                EventoTransaccionalDTO dto = EventoTransaccionalDTO.crear(
                                usuarioId,
                                entidadId,
                                "MICROSERVICIO-NUCLEO-FINANCIERO",
                                "TRANSACCION",
                                "REGISTRO_MOVIMIENTO",
                                null,
                                valorNuevo);

                super.publicarTransaccion(dto, "transaccion");
        }

        /**
         * Publica un evento de acceso o lectura de informaciÃ³n sensible.
         * <p>
         * Se utiliza para registrar consultas de historial, resÃºmenes financieros y
         * reportes.
         * </p>
         * 
         * @param usuarioId ID del usuario que accede a la informaciÃ³n.
         * @param accion    Etiqueta de la acciÃ³n realizada (ej: CONSULTA_HISTORIAL).
         * @param mensaje   Detalle descriptivo o rango de bÃºsqueda.
         * @param ip        DirecciÃ³n IP de origen.
         */
        public void publicarAcceso(UUID usuarioId, String accion, String mensaje, String ip) {
                // Combinamos la acciÃ³n con el mensaje para no perder metadata en el DTO oficial
                String detalleCompleto = String.format("[%s] %s", accion, mensaje);

                EventoAccesoDTO dto = EventoAccesoDTO.de(
                                usuarioId,
                                ip,
                                EstadoEvento.EXITO,
                                detalleCompleto,
                                null);

                super.publicarAcceso(dto, EstadoEvento.EXITO);
        }
}

package com.mensajeria.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mensajeria.dominio.entidades.BandejaSalidaMensajeria;
import com.mensajeria.dominio.repositorios.RepositorioBandejaSalidaMensajeria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicador de auditorÃ­a para mensajerÃ­a.
 * Hereda de la base para aprovechar el enrutamiento estÃ¡ndar.
 * 
 * @version 1.2.0
 */
@Component
@Slf4j
public class PublicadorAuditoria extends PublicadorEventosBase {

    private final RepositorioBandejaSalidaMensajeria outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructor con inyecciÃ³n de dependencias.
     */
    public PublicadorAuditoria(RabbitTemplate rabbitTemplate, 
                               RepositorioBandejaSalidaMensajeria outboxRepository,
                               ObjectMapper objectMapper) {
        super(rabbitTemplate);
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica un evento de seguridad de mensajerÃ­a usando PatrÃ³n Outbox.
     * 
     * @param usuario id del usuario
     * @param accion  acciÃ³n realizada
     * @param detalle detalle adicional
     */
    @Transactional
    public void publicarEventoSeguridad(UUID usuario, String accion, String detalle) {
        EventoAuditoriaDTO dto = EventoAuditoriaDTO.crear(
                usuario,
                accion,
                com.mensajeria.MicroservicioMensajeriaApplication.NOMBRE_SERVICIO,
                "INTERNAL",
                detalle);

        try {
            // 1. Guardar en Bandeja de Salida (dentro de la misma transacciÃ³n de negocio)
            String payload = objectMapper.writeValueAsString(dto);
            BandejaSalidaMensajeria outbox = BandejaSalidaMensajeria.builder()
                    .tipoEvento(".seguridad")
                    .payload(payload)
                    .build();
            outbox = outboxRepository.save(outbox);

            // 2. Intentar enviar a RabbitMQ
            super.publicarEvento(dto, ".seguridad");

            // 3. Si tiene Ã©xito inmediato, marcar como procesado
            outbox.setProcesado(true);
            outboxRepository.save(outbox);
            
        } catch (Exception e) {
            log.error("[OUTBOX] Error al publicar evento de auditorÃ­a inmediatamente, se reintentarÃ¡ luego: {}", e.getMessage());
            // No relanzar excepciÃ³n, permitir que la transacciÃ³n de negocio principal se complete
        }
    }
}

package com.cliente.infraestructura.mensajeria;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publicador especializado para la sincronizaciÃ³n en tiempo real del
 * contexto financiero del cliente hacia el microservicio-ia.
 * <p>
 * Extiende de {@link PublicadorEventosBase} y encapsula la lÃ³gica de envÃ­o
 * del {@link ContextoEstrategicoIADTO} completo a travÃ©s de RabbitMQ.
 * El mensaje se publica en {@code exchange.cliente.actualizaciones} con
 * routing key {@code cliente.perfil.actualizado}, y es consumido por el
 * {@code EscuchadorSincronizacionIA} en ms-ia, quien actualiza la cachÃ©
 * Redis {@code ia:contexto:{usuarioId}} sin necesidad de consultar la DB.
 * </p>
 *
 * <h3>Flujo completo:</h3>
 * <pre>
 * ms-cliente (escritura) â†’ PublicadorSincronizacionIA â†’ RabbitMQ
 *     â†’ cola.ia.sincronizacion.contexto â†’ ms-ia (Python)
 *     â†’ Redis (ia:contexto:{usuarioId})
 * </pre>
 *
 * @version 1.1.0
 * @since 2026-05-10
 */
@Slf4j
@Component
public class PublicadorSincronizacionIA extends PublicadorEventosBase {

    /**
     * Constructor que inyecta el RabbitTemplate a la clase base.
     *
     * @param rabbitTemplate Cliente de RabbitMQ configurado por Spring.
     */
    public PublicadorSincronizacionIA(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Publica el contexto estratÃ©gico actualizado del cliente de forma asÃ­ncrona.
     * <p>
     * Este mÃ©todo debe invocarse tras cualquier operaciÃ³n de escritura
     * (crear, actualizar, eliminar) en los servicios de perfil, metas o lÃ­mites.
     * El mensaje contiene el DTO completo para que el consumidor no tenga que
     * realizar consultas adicionales a la base de datos. El {@code usuarioId}
     * se inyecta como header AMQP para que el consumidor identifique la clave Redis.
     * </p>
     *
     * @param usuarioId ID del usuario propietario del contexto.
     * @param contexto  DTO ligero con el contexto financiero optimizado para IA.
     */
    public void publicarActualizacionContexto(UUID usuarioId, ContextoEstrategicoIADTO contexto) {
        log.info("[SYNC-IA] Publicando contexto actualizado para usuarioId={}, nombres='{}'",
                usuarioId, contexto.nombres());
        this.publicarSincronizacionCliente(contexto, usuarioId.toString());
    }
}

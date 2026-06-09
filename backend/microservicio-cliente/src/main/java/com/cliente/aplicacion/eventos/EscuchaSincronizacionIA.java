package com.cliente.aplicacion.eventos;

import com.cliente.aplicacion.puertos.ServicioContexto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener transaccional que reacciona a los cambios en el contexto
 * financiero del usuario <b>despuÃ©s de que la transacciÃ³n de BD haya
 * sido confirmada</b> (AFTER_COMMIT).
 * <p>
 * Implementa el patrÃ³n <i>Transactional Event Publisher</i>:
 * los servicios de negocio publican un {@link EventoContextoActualizado}
 * mediante {@code ApplicationEventPublisher}, y este listener lo captura
 * Ãºnicamente si el commit fue exitoso. Esto elimina el riesgo de enviar
 * mensajes a RabbitMQ/Redis sobre datos que podrÃ­an haber sido revertidos
 * por un rollback.
 * </p>
 *
 * <h3>Flujo:</h3>
 * <pre>
 * Servicio (dentro de @Transactional)
 *   â””â”€ eventPublisher.publishEvent(new EventoContextoActualizado(...))
 *
 * BD confirma (COMMIT)
 *   â””â”€ EscuchaSincronizacionIA.alActualizarContexto()
 *       â”œâ”€ ServicioContexto.refrescarContextoRedis(usuarioId)
 *       â”‚   â”œâ”€ Actualiza Redis (ia:contexto:{uuid})
 *       â”‚   â””â”€ Publica a RabbitMQ (exchange.cliente.actualizaciones)
 *       â””â”€ Log de trazabilidad
 * </pre>
 *
 * @version 1.1.0
 * @since 2026-05-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscuchaSincronizacionIA {

    private final ServicioContexto servicioContexto;

    /**
     * Reacciona al evento de contexto actualizado despuÃ©s del commit.
     * <p>
     * Dispara el refresco de la cachÃ© Redis y la publicaciÃ³n del
     * mensaje de sincronizaciÃ³n a RabbitMQ de forma secuencial y
     * segura despuÃ©s del commit de la transacciÃ³n.
     * </p>
     *
     * @param evento Evento con el ID del usuario y el origen del cambio.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alActualizarContexto(EventoContextoActualizado evento) {
        log.info("[SYNC-LISTENER] TransacciÃ³n confirmada. Sincronizando contexto IA " +
                "para usuarioId={} (origen: {})", evento.getUsuarioId(), evento.getOrigen());
        servicioContexto.refrescarContextoRedis(evento.getUsuarioId());
    }
}

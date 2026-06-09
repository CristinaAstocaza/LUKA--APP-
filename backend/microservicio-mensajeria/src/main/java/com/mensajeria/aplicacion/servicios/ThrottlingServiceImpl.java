package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.excepciones.LimiteIntentosExcedidoException;
import com.mensajeria.aplicacion.puertos.IThrottlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * ImplementaciÃ³n del servicio de throttling por canal usando Redis como almacÃ©n
 * de contadores con TTL diario.
 * <p>
 * La estructura de clave en Redis es:
 * {@code luka:throttling:{canal}:{identificador}}
 * donde {@code canal} puede ser {@code email} o {@code sms}, e
 * {@code identificador} es el valor Ãºnico del usuario en ese canal.
 * El contador se incrementa en cada intento; al superar 3 se lanza
 * {@link LimiteIntentosExcedidoException}. El TTL se fija a las 00:00:00
 * del dÃ­a siguiente.
 * </p>
 *
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThrottlingServiceImpl implements IThrottlingService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_INTENTOS = 3;
    private static final String PREFIJO_CLAVE = "luka:throttling:";

    /**
     * {@inheritDoc}
     * <p>
     * Si la clave no existÃ­a (primer intento del dÃ­a), configura su TTL para
     * expirar exactamente a las 00:00:00 de la zona horaria local.
     * </p>
     *
     * @param canal         Canal de notificaciÃ³n ({@code "email"} o {@code "sms"}).
     * @param identificador Email, telÃ©fono o UUID del usuario en ese canal.
     * @throws LimiteIntentosExcedidoException cuando los intentos acumulados superan 3.
     */
    @Override
    @SuppressWarnings("null")
    public void registrarIntento(String canal, String identificador) {
        String clave = PREFIJO_CLAVE + canal.toLowerCase() + ":" + identificador;
        Long intentos = redisTemplate.opsForValue().increment(clave);

        if (intentos != null && intentos == 1) {
            Instant medianoche = LocalDate.now(ZoneId.systemDefault())
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
            redisTemplate.expireAt(clave, medianoche);
            log.debug("[THROTTLING] Clave '{}' creada, expira a medianoche.", clave);
        }

        log.debug("[THROTTLING] Canal={}, id={}, intentos={}/{}", canal, identificador, intentos, MAX_INTENTOS);

        if (intentos != null && intentos > MAX_INTENTOS) {
            log.warn("[THROTTLING] LÃ­mite superado para canal='{}', id='{}'", canal, identificador);
            throw new LimiteIntentosExcedidoException(canal);
        }
    }
}

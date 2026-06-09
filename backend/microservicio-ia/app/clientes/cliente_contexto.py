"""
clientes/cliente_contexto.py  Â·  v1.0
â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
Cliente HTTP para reconstrucciÃ³n de cachÃ© (Plan B / Pull Fallback).

Si al procesar una solicitud de chat la clave Redis `ia:contexto:{usuarioId}`
estÃ¡ vacÃ­a, este cliente realiza una llamada HTTP GET al endpoint interno
del ms-cliente para obtener el ContextoEstrategicoIADTO y reconstruir la
cachÃ© antes de responder.

Endpoint:
    GET {url_cliente}/api/v1/clientes/interno/contexto-financiero/{usuarioId}

@version 1.1.0
@since 2026-05-10
â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
"""

import json
import logging
from typing import Optional

import httpx

from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)
config = obtener_configuracion()

REDIS_KEY_PREFIX = "ia:contexto:"
REDIS_TTL_SECONDS = 3600


class ClienteContexto:
    """
    Cliente HTTP sÃ­ncrono para obtener el contexto financiero del ms-cliente.
    Implementa el patrÃ³n "Pull Fallback": si Redis no tiene el contexto
    del usuario, este cliente consulta al ms-cliente para reconstruirlo.
    """

    def __init__(self, redis_client=None):
        """
        Inicializa el cliente con la URL del ms-cliente y un cliente Redis
        opcional para escritura de cachÃ©.

        Args:
            redis_client: Instancia de redis.Redis (opcional). Si se provee,
                          el resultado se cachea en Redis automÃ¡ticamente.
        """
        self.url_base = config.url_cliente
        self.timeout = httpx.Timeout(15.0, connect=5.0)
        self._redis = redis_client

    def obtener_contexto_ia(
        self, usuario_id: str, token: str
    ) -> Optional[dict]:
        """
        Obtiene el contexto estratÃ©gico de IA para un usuario.

        Flujo:
            1. Consulta Redis (`ia:contexto:{usuarioId}`).
            2. Si hay cache hit â†’ retorna el JSON parseado.
            3. Si hay cache miss â†’ consulta HTTP al ms-cliente.
            4. Si el HTTP responde â†’ cachea en Redis y retorna.
            5. Si el HTTP falla â†’ retorna None (degradaciÃ³n elegante).

        Args:
            usuario_id: UUID del usuario en formato string.
            token:      JWT del usuario para autenticaciÃ³n inter-servicio.

        Returns:
            Diccionario con el contexto financiero, o None si no disponible.
        """
        # â”€â”€ 1. Intentar desde Redis (cache hit) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if self._redis:
            try:
                redis_key = f"{REDIS_KEY_PREFIX}{usuario_id}"
                cached = self._redis.get(redis_key)
                if cached:
                    logger.info(
                        "[CONTEXTO-PULL] Cache HIT para usuario=%s",
                        usuario_id,
                    )
                    return json.loads(cached)
                logger.info(
                    "[CONTEXTO-PULL] Cache MISS para usuario=%s â€” "
                    "realizando consulta HTTP al ms-cliente.",
                    usuario_id,
                )
            except Exception as exc:
                logger.warning(
                    "[CONTEXTO-PULL] Error leyendo Redis: %s. "
                    "Continuando con consulta HTTP.",
                    exc,
                )

        # â”€â”€ 2. Consulta HTTP al ms-cliente (Plan B) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        url = (
            f"{self.url_base}/api/v1/clientes/interno/"
            f"contexto-financiero/{usuario_id}"
        )
        headers = {
            "Authorization": f"Bearer {token}",
            "X-Gateway-Source": "api-gateway"
        }

        try:
            with httpx.Client(timeout=self.timeout) as cliente:
                respuesta = cliente.get(url, headers=headers)
                respuesta.raise_for_status()
                json_respuesta = respuesta.json()
                contexto = json_respuesta.get("datos", {})

                logger.info(
                    "[CONTEXTO-PULL] Contexto obtenido vÃ­a HTTP para "
                    "usuario=%s â€” nombres='%s'",
                    usuario_id,
                    contexto.get("nombres", "N/A"),
                )

                # â”€â”€ 3. Cachear en Redis para futuras consultas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if self._redis:
                    try:
                        redis_key = f"{REDIS_KEY_PREFIX}{usuario_id}"
                        self._redis.setex(
                            name=redis_key,
                            time=REDIS_TTL_SECONDS,
                            value=json.dumps(contexto, ensure_ascii=False),
                        )
                        logger.info(
                            "[CONTEXTO-PULL] CachÃ© reconstruida: %s (TTL=%ds)",
                            redis_key,
                            REDIS_TTL_SECONDS,
                        )
                    except Exception as exc:
                        logger.warning(
                            "[CONTEXTO-PULL] Error escribiendo en Redis: %s. "
                            "El contexto se usarÃ¡ sin cachear.",
                            exc,
                        )

                return contexto

        except httpx.ConnectError:
            logger.error(
                "[CONTEXTO-PULL] No se pudo conectar al ms-cliente en %s. "
                "El chat continuarÃ¡ sin contexto personalizado.",
                self.url_base,
            )
            return None
        except httpx.TimeoutException:
            logger.error(
                "[CONTEXTO-PULL] Timeout al consultar contexto para "
                "usuario=%s.",
                usuario_id,
            )
            return None
        except httpx.HTTPStatusError as exc:
            logger.error(
                "[CONTEXTO-PULL] HTTP %d al obtener contexto: %s",
                exc.response.status_code,
                exc.response.text,
            )
            return None
        except Exception as exc:
            logger.error(
                "[CONTEXTO-PULL] Error inesperado: %s",
                exc,
                exc_info=True,
            )
            return None

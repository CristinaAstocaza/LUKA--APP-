import unittest
from unittest.mock import MagicMock, AsyncMock, patch
import sys
import os

# Configurar path para importar módulos locales de "app"
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.modelos.esquemas import SolicitudClasificacionDTO, CategoriaSugeridaDTO
from app.servicios.ia.clasificador_ia import ClasificadorIAService

class Test_Microservicio_IA(unittest.IsolatedAsyncTestCase):
    """
    Pruebas unitarias para el clasificador inteligente de transacciones (Gemini).
    Verifica que la clasificación retorne predicciones correctas o active fallbacks seguros.
    """

    def setUp(self):
        # Mock de configuración global de la aplicación
        self.mock_config = MagicMock()
        self.mock_config.gemini_api_key = "fake-api-key"
        self.mock_config.gemini_modelo = "gemini-1.5-flash"
        
        # Parches para evitar inicializar APIs reales de Google/Gemini
        self.patch_config = patch("app.servicios.ia.clasificador_ia.obtener_configuracion", return_value=self.mock_config)
        self.patch_genai = patch("google.generativeai.configure")
        self.patch_model = patch("google.generativeai.GenerativeModel")

        self.patch_config.start()
        self.patch_genai.start()
        self.mock_model_class = self.patch_model.start()

        # Instanciar el servicio bajo prueba
        self.servicio = ClasificadorIAService()

    def tearDown(self):
        # Detener parches mock
        self.patch_config.stop()
        self.patch_genai.stop()
        self.patch_model.stop()

    async def test_clasificar_sin_contexto_retorna_fallback(self):
        """
        Caso 1: Si no hay descripción ni etiquetas (contexto vacío),
        debe retornar las sugerencias de contingencia (fallback) de forma inmediata.
        """
        solicitud = SolicitudClasificacionDTO(
            id_temporal="temp-123",
            tipo_movimiento="GASTO",
            descripcion="",
            etiquetas=""
        )

        resultado = await self.servicio.clasificar(solicitud)

        # Verificaciones
        self.assertEqual(resultado.id_temporal, "temp-123")
        self.assertTrue(resultado.usando_fallback)
        self.assertEqual(len(resultado.sugerencias), 5)
        # La primera sugerencia por defecto para GASTOS debe ser Alimentos
        self.assertEqual(resultado.sugerencias[0].categoria, "Alimentos")

    async def test_clasificar_con_error_api_retorna_fallback(self):
        """
        Caso 2: Si la API de Gemini falla (por ejemplo, cuota excedida o red),
        el servicio debe capturar el error y retornar el fallback de forma segura.
        """
        solicitud = SolicitudClasificacionDTO(
            id_temporal="temp-456",
            tipo_movimiento="INGRESO",
            descripcion="Pago de honorarios consultoría",
            etiquetas="freelance"
        )

        # Simulamos que Gemini lanza un error de conexión
        self.servicio.model.generate_content_async = AsyncMock(side_effect=Exception("API Error"))

        resultado = await self.servicio.clasificar(solicitud)

        # Verificaciones
        self.assertTrue(resultado.usando_fallback)
        self.assertEqual(len(resultado.sugerencias), 5)
        # Para INGRESO, el primer fallback debe ser Salario
        self.assertEqual(resultado.sugerencias[0].categoria, "Salario")

    async def test_clasificar_con_exito(self):
        """
        Caso 3: Cuando Gemini responde exitosamente con el formato JSON esperado.
        El servicio debe deserializar y retornar exactamente estas 5 sugerencias.
        """
        solicitud = SolicitudClasificacionDTO(
            id_temporal="temp-789",
            tipo_movimiento="GASTO",
            descripcion="Cena en restaurante de carnes",
            etiquetas="comida"
        )

        # Respuesta simulada en formato JSON que Gemini Structured Output devolvería
        mock_response_text = """
        {
            "sugerencias": [
                {"categoria": "Restaurantes", "icono": "utensils"},
                {"categoria": "Alimentos", "icono": "utensils"},
                {"categoria": "Entretenimiento", "icono": "film"},
                {"categoria": "Salidas", "icono": "glass-cheers"},
                {"categoria": "Otros", "icono": "receipt"}
            ]
        }
        """
        
        mock_response = MagicMock()
        mock_response.text = mock_response_text
        self.servicio.model.generate_content_async = AsyncMock(return_value=mock_response)

        resultado = await self.servicio.clasificar(solicitud)

        # Verificaciones
        self.assertFalse(resultado.usando_fallback)
        self.assertEqual(len(resultado.sugerencias), 5)
        self.assertEqual(resultado.sugerencias[0].categoria, "Restaurantes")
        self.assertEqual(resultado.sugerencias[0].icono, "utensils")

if __name__ == "__main__":
    unittest.main()

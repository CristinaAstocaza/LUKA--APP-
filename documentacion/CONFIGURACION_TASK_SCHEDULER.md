# Configuración de Mantenimiento Automático (Task Scheduler)

Esta guía explica cómo automatizar la ejecución del script de mantenimiento (`mantenimiento.ps1`) utilizando el Programador de tareas (Task Scheduler) nativo de Windows.

## 1. Crear una tarea programada en Windows

1. Abre el menú Inicio de Windows, escribe **"Programador de tareas"** (o "Task Scheduler" si está en inglés) y abre la aplicación.
2. En el panel de la derecha, haz clic en **"Crear tarea..."** (Atención: *NO uses "Crear tarea básica"*).

## 2. Configuración General (Pestaña "General")

1. **Nombre:** Escribe un nombre descriptivo, por ejemplo: `LukaApp - Mantenimiento Backend`.
2. **Descripción:** (Opcional) "Ejecución del script principal de mantenimiento y backup de base de datos."
3. **Opciones de seguridad (MUY IMPORTANTE):**
   * Selecciona la opción: **"Ejecutar tanto si el usuario ha iniciado sesión como si no"**. Esto garantiza que el script corra de madrugada aunque la computadora esté bloqueada o sin sesión iniciada.
   * Marca la casilla: **"Ejecutar con los privilegios más altos"**. (Docker y PowerShell a menudo requieren permisos de administrador para interactuar sin interrupciones).
4. Configurar para: Selecciona tu versión de Windows (ej. Windows 10/11).

## 3. Configuración del Horario (Pestaña "Desencadenadores")

1. Haz clic en **"Nuevo..."**.
2. **Iniciar la tarea:** Según una programación.
3. Selecciona **"Diariamente"**.
4. **Hora sugerida:** Configura la hora para las **03:00 AM**. 
   *(Las 3:00 AM es un horario recomendado porque el tráfico de usuarios en la plataforma es prácticamente nulo y evitas afectar el rendimiento del sistema).*
5. Asegúrate de que la casilla "Habilitado" esté marcada y haz clic en Aceptar.

## 4. Configurar el Script a Ejecutar (Pestaña "Acciones")

1. Haz clic en **"Nuevo..."**.
2. **Acción:** Iniciar un programa.
3. **Programa o script:** Escribe `powershell.exe`
4. **Agregar argumentos (Opcional):** 
   Aquí es donde autorizamos la ejecución del script y le pasamos el archivo. Escribe exactamente lo siguiente (asegúrate de que la ruta absoluta coincida con la ubicación real de tu proyecto):
   
   `-ExecutionPolicy Bypass -WindowStyle Hidden -File "D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\estructura-backend\scripts\mantenimiento.ps1"`

5. **Iniciar en (Opcional):** Puedes colocar la ruta de la carpeta (sin comillas):
   `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\estructura-backend\scripts\`

## 5. Guardar y probar manualmente la tarea

1. Ve a la pestaña **"Condiciones"** y desmarca la opción que dice "Iniciar la tarea solo si el equipo está conectado a la corriente alterna" (especialmente si es una laptop que actúa como servidor).
2. Haz clic en **"Aceptar"** para guardar la tarea. Te pedirá la contraseña de tu usuario de Windows (necesario por haber elegido "Ejecutar aunque el usuario no haya iniciado sesión").
3. Para probar que todo está bien configurado: en la lista de Tareas, haz clic derecho sobre tu nueva tarea `LukaApp - Mantenimiento Backend` y selecciona **"Ejecutar"**.

## Resumen Rápido de Configuración

**Frecuencia:**
- Diariamente

**Hora:**
- 03:00 AM

**Programa:**
`powershell.exe`

**Argumentos:**
`-ExecutionPolicy Bypass -WindowStyle Hidden -File "D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\estructura-backend\scripts\mantenimiento.ps1"`

**Iniciar en:**
`D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\estructura-backend\scripts`

## Validación

✓ Verificar que se creó un nuevo backup.
✓ Verificar mantenimiento.log.
✓ Verificar que la tarea terminó correctamente.

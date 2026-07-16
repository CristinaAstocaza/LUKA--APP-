<#
.SYNOPSIS
    Script principal de mantenimiento para la Plataforma de Microservicios (Backend).
.DESCRIPTION
    Este script inicializa la infraestructura para las tareas de mantenimiento, 
    creando los directorios necesarios y manejando el registro de logs.
    Esta preparado para ser escalado con modulos de backup, limpieza y purga de datos.
#>

$ErrorActionPreference = "Stop"

# Constante de metadatos (informativa)
$EXECUTION_MODE = "Soporta ejecucion MANUAL o programada (TASK_SCHEDULER/CRON)"

# Rutas principales relativas a la ubicacion del script
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendRoot = Resolve-Path (Join-Path $ScriptDir "..")
$BackupsDir = Join-Path $BackendRoot "backups"
$LogFile = Join-Path $ScriptDir "mantenimiento.log"

# ==============================================================================
# FUNCIONES AUXILIARES
# ==============================================================================

function Write-Log {
    param([string]$Message)
    
    # Escribe en el archivo de log (Add-Content no sobrescribe, siempre agrega al final)
    Add-Content -Path $LogFile -Value $Message
    # Muestra en consola
    Write-Host $Message
}

function Initialize-BackupFolder {
    if (-not (Test-Path -Path $BackupsDir)) {
        Write-Log "La carpeta destinada a backups no existe."
        Write-Log "Creando directorio en: $BackupsDir"
        New-Item -ItemType Directory -Force -Path $BackupsDir | Out-Null
        Write-Log "Carpeta de backups creada exitosamente."
    } else {
        Write-Log "Carpeta de backups verificada correctamente en: $BackupsDir"
    }
}

# ==============================================================================
# FUNCIONES DE MANTENIMIENTO FUTURAS (PREPARATIVOS)
# ==============================================================================

# region Tareas de Mantenimiento (Pendientes de implementar)

function Backup-PostgreSQL {
    try {
        Write-Log "Iniciando respaldo de PostgreSQL..."
        $StartTime = Get-Date

        # Verificar si Docker esta ejecutandose
        docker info > $null 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Docker no esta ejecutandose o no es accesible."
        }

        # Verificar si el contenedor de PostgreSQL esta activo
        $ContainerName = "luka-postgres-infra"
        $containerStatus = docker inspect -f '{{.State.Running}}' $ContainerName 2>&1
        if ($LASTEXITCODE -ne 0 -or $containerStatus -notmatch "true") {
            throw "El contenedor '$ContainerName' no se encuentra en ejecucion."
        }

        # Formatear el nombre del archivo de backup
        $Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
        $BackupFileName = "lukaapp_$Timestamp.sql"
        $BackupFilePath = Join-Path $BackupsDir $BackupFileName

        # Ejecutamos pg_dumpall a traves de cmd para evitar conflictos de encoding UTF-16LE de PowerShell
        $process = Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"docker exec $ContainerName pg_dumpall -U postgres > `"$BackupFilePath`"`"" -Wait -NoNewWindow -PassThru
        
        if ($process.ExitCode -ne 0) {
            throw "El proceso de backup devolvio codigo de error: $($process.ExitCode)"
        }

        # Verificar existencia y tamanio del archivo
        if (-not (Test-Path -Path $BackupFilePath) -or (Get-Item $BackupFilePath).Length -eq 0) {
            throw "El archivo de backup no se genero correctamente o esta vacio."
        }

        $EndTime = Get-Date
        $Duration = ($EndTime - $StartTime).TotalSeconds
        
        Write-Log "Archivo generado: $BackupFileName"
        Write-Log "Duracion del backup: $([math]::Round($Duration, 2)) segundos."
    }
    catch {
        # Se captura la excepcion para no detener el script abruptamente
        Write-Log "ERROR (Backup-PostgreSQL): $_"
    }
}

function Restore-Backup {
    # TODO: Implementar restauracion de backups de PostgreSQL.
}

function Clean-TempFiles {
    # TODO: Implementar logica para eliminar archivos temporales del sistema.
}

function Clean-ExpiredData {
    # TODO: Implementar invocacion a la API (Spring Scheduler) o scripts para purgar datos caducados.
}

# endregion

# ==============================================================================
# PUNTO DE ENTRADA PRINCIPAL
# ==============================================================================

function Main {
    $CurrentTime = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

    try {
        Write-Log "================================================="
        Write-Log "Inicio de mantenimiento: $CurrentTime"
        Write-Log "================================================="

        # 1. Preparacion de entorno
        Initialize-BackupFolder

        # 2. Ejecucion de modulos de mantenimiento (Marcadores)
        
        Backup-PostgreSQL
        # Restore-Backup
        # Clean-TempFiles
        # Clean-ExpiredData

        $EndTime = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Write-Log "================================================="
        Write-Log "Fin de mantenimiento: $EndTime"
        Write-Log "================================================="
    }
    catch {
        $ErrorTime = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Write-Log "ERROR DURANTE EL MANTENIMIENTO: $_"
        Write-Log "================================================="
        Write-Log "Fin de mantenimiento (con error): $ErrorTime"
        Write-Log "================================================="
        exit 1
    }
}

# Ejecucion del script
Main

# Migración completa de la sección Perfil (repo antiguo ➜ repo avanzado)

Este documento consolida **todo lo necesario** para migrar la feature Perfil desde este repo (antiguo) hacia tu repo avanzado, sin buscar dependencias manualmente.

---

## 1) Inventario completo de archivos de Perfil

### 1.1 Feature principal

- `src/app/features/perfil/perfil.routes.ts`
- `src/app/features/perfil/perfil-layout/perfil-layout.ts`
- `src/app/features/perfil/perfil-layout/perfil-layout.html`
- `src/app/features/perfil/perfil-layout/perfil-layout.scss`
- `src/app/features/perfil/perfil-layout/perfil-layout.spec.ts`

### 1.2 Perfil cliente

- `src/app/features/perfil/perfil-cliente/perfil-cliente.ts`
- `src/app/features/perfil/perfil-cliente/perfil-cliente.html`
- `src/app/features/perfil/perfil-cliente/perfil-cliente.scss`
- `src/app/features/perfil/perfil-cliente/perfil-cliente.spec.ts`

### 1.3 Componentes internos de avatar

- `src/app/features/perfil/perfil-cliente/components/avatar-display/avatar-display.ts`
- `src/app/features/perfil/perfil-cliente/components/avatar-display/avatar-display.html`
- `src/app/features/perfil/perfil-cliente/components/avatar-display/avatar-display.scss`
- `src/app/features/perfil/perfil-cliente/components/avatar-selector/avatar-selector.ts`
- `src/app/features/perfil/perfil-cliente/components/avatar-selector/avatar-selector.html`
- `src/app/features/perfil/perfil-cliente/components/avatar-selector/avatar-selector.scss`

### 1.4 Otras subpáginas de Perfil

- `src/app/features/perfil/perfil-financiero/perfil-financiero.ts`
- `src/app/features/perfil/perfil-financiero/perfil-financiero.html`
- `src/app/features/perfil/perfil-financiero/perfil-financiero.scss`
- `src/app/features/perfil/perfil-financiero/perfil-financiero.spec.ts`
- `src/app/features/perfil/configuracion/configuracion.ts`
- `src/app/features/perfil/configuracion/configuracion.html`
- `src/app/features/perfil/configuracion/configuracion.scss`
- `src/app/features/perfil/configuracion/configuracion.spec.ts`
- `src/app/features/perfil/historial/historial.ts`
- `src/app/features/perfil/historial/historial.html`
- `src/app/features/perfil/historial/historial.scss`
- `src/app/features/perfil/historial/historial.spec.ts`
- `src/app/features/perfil/transacciones/transacciones.ts`
- `src/app/features/perfil/transacciones/transacciones.html`
- `src/app/features/perfil/transacciones/transacciones.scss`
- `src/app/features/perfil/transacciones/transacciones.spec.ts`

### 1.5 Dependencias de servicios/models usadas por Perfil

- `src/app/core/services/avatar.service.ts`
- `src/app/core/services/cliente-perfil.service.ts`
- `src/app/core/services/auth.service.ts` *(ya existe en este repo, validar en destino)*
- `src/app/core/services/index.ts`
- `src/app/core/models/cliente/perfil-cliente.model.ts`
- `src/app/core/models/cliente/index.ts`
- `src/app/core/models/index.ts`
- `src/app/core/models/auth/user.model.ts` *(por `SolicitudCambioPassword`)*

### 1.6 Assets específicos (avatar)

- `src/assets/avatares/figuras/COLIBRI MARAVILLOSO.png`
- `src/assets/avatares/figuras/CONDOR ANDINO.png`
- `src/assets/avatares/figuras/DELFIN ROSADO.png`
- `src/assets/avatares/figuras/GATO ANDINO.png`
- `src/assets/avatares/figuras/MONO DE COLA AMARILLA.png`
- `src/assets/avatares/figuras/OSO ANDINO.png`
- `src/assets/avatares/figuras/PAVA ALIBLANCA.png`
- `src/assets/avatares/figuras/RANA GIGANTE.png`
- `src/assets/avatares/accesorios/CORBATA.png`
- `src/assets/avatares/accesorios/GORRO.png`
- `src/assets/avatares/accesorios/LENTES.png`

### 1.7 Guards

- En esta implementación de Perfil **no hay guard dedicado** en `features/perfil`.
- La protección depende del enrutado global y sesión autenticada (`AuthService`).

---

## 2) Código fuente clave a migrar

> Recomendación: copiar el árbol completo `features/perfil` y luego ajustar imports según estructura del repo avanzado.

### 2.1 Rutas de Perfil

Archivo: `src/app/features/perfil/perfil.routes.ts`

```ts
import { Routes } from '@angular/router';
import { PerfilLayout } from './perfil-layout/perfil-layout';

export const PERFIL_ROUTES: Routes = [
  {
    path: '',
    component: PerfilLayout,
    children: [
      { path: '', redirectTo: 'cliente', pathMatch: 'full' },
      {
        path: 'cliente',
        data: {
          title: 'Mi perfil',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Mi perfil' }
          ]
        },
        loadComponent: () =>
          import('./perfil-cliente/perfil-cliente')
            .then(m => m.PerfilCliente)
      },
      {
        path: 'financiero',
        data: {
          title: 'Perfil financiero',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Perfil financiero' }
          ]
        },
        loadComponent: () =>
          import('./perfil-financiero/perfil-financiero')
            .then(m => m.PerfilFinanciero)
      },
      {
        path: 'configuracion',
        data: {
          title: 'Configuración',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Configuración' }
          ]
        },
        loadComponent: () =>
          import('./configuracion/configuracion')
            .then(m => m.Configuracion)
      },
      {
        path: 'historial',
        data: {
          title: 'Historial',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Historial' }
          ]
        },
        loadComponent: () =>
          import('./historial/historial')
            .then(m => m.Historial)
      },
      {
        path: 'transacciones',
        data: {
          title: 'Transacciones',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Transacciones' }
          ]
        },
        loadComponent: () =>
          import('./transacciones/transacciones')
            .then(m => m.Transacciones)
      },
    ]
  }
];
```

### 2.2 Servicios y modelos mínimos obligatorios

Archivo: `src/app/core/services/avatar.service.ts`

```ts
import { Injectable, signal } from '@angular/core';

export interface AvatarConfig {
  figura: string;
  accesorio?: string;
}

@Injectable({ providedIn: 'root' })
export class AvatarService {
  private readonly storageKey = 'lukaapp.avatar.config';

  readonly avatarConfig = signal<AvatarConfig>({
    figura: 'GATO ANDINO',
    accesorio: 'LENTES',
  });

  setAvatar(config: AvatarConfig): void {
    this.avatarConfig.set(config);
    localStorage.setItem(this.storageKey, JSON.stringify(config));
  }

  getAvatar(): AvatarConfig {
    return this.avatarConfig();
  }

  loadAvatar(): void {
    const rawConfig = localStorage.getItem(this.storageKey);
    if (!rawConfig) {
      return;
    }

    try {
      const parsed = JSON.parse(rawConfig) as AvatarConfig;
      if (parsed?.figura) {
        this.avatarConfig.set(parsed);
      }
    } catch {
      localStorage.removeItem(this.storageKey);
    }
  }
}
```

Archivo: `src/app/core/services/cliente-perfil.service.ts`

```ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import {
  RespuestaDatosPersonales,
  RespuestaPerfilFinanciero,
  SolicitudDatosPersonales,
  SolicitudPerfilFinanciero
} from '../models/cliente/perfil-cliente.model';

@Injectable({ providedIn: 'root' })
export class ClientePerfilService {
  private basePerfil = `${environment.gatewayUrl}/api/v1/clientes/perfil`;
  private basePerfilFinanciero = `${environment.gatewayUrl}/api/v1/clientes/perfil-financiero`;

  constructor(private http: HttpClient) {}

  crearPerfilInicial(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.http.post<RespuestaDatosPersonales>(`${this.basePerfil}/inicial?usuarioId=${usuarioId}`, {});
  }

  consultarPerfil(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.http.get<RespuestaDatosPersonales>(`${this.basePerfil}/${usuarioId}`);
  }

  obtenerPerfil(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.consultarPerfil(usuarioId);
  }

  actualizarPerfil(usuarioId: string, payload: SolicitudDatosPersonales): Observable<RespuestaDatosPersonales> {
    return this.http.put<RespuestaDatosPersonales>(`${this.basePerfil}/${usuarioId}`, payload);
  }

  consultarPerfilFinanciero(usuarioId: string): Observable<RespuestaPerfilFinanciero> {
    return this.http.get<RespuestaPerfilFinanciero>(`${this.basePerfilFinanciero}/${usuarioId}`);
  }

  guardarPerfilFinanciero(usuarioId: string, payload: SolicitudPerfilFinanciero): Observable<RespuestaPerfilFinanciero> {
    return this.http.put<RespuestaPerfilFinanciero>(`${this.basePerfilFinanciero}/${usuarioId}`, payload);
  }
}
```

Archivo: `src/app/core/models/cliente/perfil-cliente.model.ts`

```ts
export interface SolicitudDatosPersonales {
  dni: string;
  nombres: string;
  apellidos: string;
  genero: string;
  edad: number;
  telefono: string;
  fotoPerfilUrl?: string;
  direccion?: string;
  ciudad?: string;
}

export interface RespuestaDatosPersonales {
  dni: string;
  nombres: string;
  apellidos: string;
  genero: string;
  edad: number;
  telefono: string;
  fotoPerfilUrl: string;
  direccion: string;
  ciudad: string;
  datosCompletos: boolean;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface SolicitudPerfilFinanciero {
  ocupacion: string;
  ingresoMensual: number;
  estiloVida: string;
  tonoIA: string;
}

export interface RespuestaPerfilFinanciero {
  ocupacion: string;
  ingresoMensual: number;
  estiloVida: string;
  tonoIA: string;
  fechaCreacion: string;
  fechaActualizacion: string;
}
```

---

## 3) Cambios requeridos en rutas/módulos/servicios al migrar

### 3.1 Rutas globales

En el destino debe existir (o agregarse) la carga lazy de Perfil en `src/app/app.routes.ts`:

```ts
{
  path: 'perfil',
  loadChildren: () =>
    import('./features/perfil/perfil.routes')
      .then(m => m.PERFIL_ROUTES)
}
```

### 3.2 Barrel exports

Verificar que estos exports existan en destino:

`src/app/core/services/index.ts`

```ts
export * from './cliente-perfil.service';
```

`src/app/core/models/cliente/index.ts`

```ts
export * from './perfil-cliente.model';
```

`src/app/core/models/index.ts`

```ts
export * from './cliente';
```

### 3.3 Configuración backend/API

- Confirmar que `environment.gatewayUrl` exista en `src/app/enviroments/environment.ts`.
- Confirmar endpoints:
  - `GET /api/v1/clientes/perfil/{usuarioId}`
  - `PUT /api/v1/clientes/perfil/{usuarioId}`
  - `GET /api/v1/clientes/perfil-financiero/{usuarioId}`
  - `PUT /api/v1/clientes/perfil-financiero/{usuarioId}`

---

## 4) Diferencias arquitectónicas y adaptación al repo avanzado

### Fuentes potenciales de falla (6)

1. Imports relativos rotos por estructura distinta (`core/services`, `core/models`).
2. Falta de assets en `assets/avatares` (nombres con espacios y `.png` en minúscula en código).
3. Diferencia en modelo de sesión (`authService.usuario()?.id` no disponible en destino).
4. `environment` con ruta distinta (`enviroments` vs `environments`).
5. Falta de export en barrels (`core/services/index.ts`, `core/models/index.ts`).
6. Contratos backend distintos para payload/respuesta de perfil.

### 2 causas más probables

- **Causa A:** imports/barrels incompletos.
- **Causa B:** incompatibilidad de contrato `AuthService` o backend de perfil.

### Logs/checks recomendados para validar diagnóstico en repo avanzado

1. Buscar referencias de perfil y imports:

```bash
findstr /s /n /i "PERFIL_ROUTES ClientePerfilService AvatarService RespuestaDatosPersonales" src\app\*.ts
```

2. Verificar assets:

```bash
dir /s /b src\assets\avatares\figuras\*.png
dir /s /b src\assets\avatares\accesorios\*.png
```

3. Build para detectar imports faltantes:

```bash
npm run build
```

4. Log temporal en `PerfilCliente`:

```ts
console.log('[PerfilCliente] usuarioSesion', this.authService.usuario());
console.log('[PerfilCliente] perfilBackend', perfil);
```

> **Confirmación solicitada antes de corregir en repo avanzado:** valida primero si la causa A o B es la que falla en tu repositorio destino.

---

## 5) Plan de migración paso a paso

### Paso 1: Archivos a copiar

1. Copiar **todo** `src/app/features/perfil/**`.
2. Copiar `src/app/core/services/avatar.service.ts`.
3. Copiar `src/app/core/services/cliente-perfil.service.ts`.
4. Copiar `src/app/core/models/cliente/perfil-cliente.model.ts`.
5. Copiar `src/assets/avatares/**`.

### Paso 2: Archivos a modificar

1. `src/app/app.routes.ts` (registrar ruta lazy de perfil).
2. `src/app/core/services/index.ts` (export de `cliente-perfil.service`).
3. `src/app/core/models/cliente/index.ts` y `src/app/core/models/index.ts`.
4. Ajustar imports relativos de Perfil según estructura real del destino.

### Paso 3: Rutas a registrar

- Ruta raíz: `/perfil`.
- Hijas: `/perfil/cliente`, `/perfil/financiero`, `/perfil/configuracion`, `/perfil/historial`, `/perfil/transacciones`.

### Paso 4: Servicios a conectar

1. `ClientePerfilService` ↔ gateway backend.
2. `AuthService` ↔ sesión actual con `usuario().id`.
3. `AvatarService` ↔ estado local de avatar + persistencia localStorage.

### Paso 5: Validaciones funcionales

1. Abrir `/perfil/cliente` y comprobar carga de datos del backend.
2. Cambiar avatar y validar persistencia UI + `PUT` perfil.
3. Editar género/teléfono/ciudad y confirmar guardado.
4. Cambiar contraseña desde Perfil Cliente.
5. Abrir `/perfil/configuracion` y validar cambio de tema/color persistido.
6. Recargar navegador y confirmar persistencia de avatar/tema.

---

## 6) Qué se reutiliza, qué se reescribe y riesgos

### Reutilizado directamente

- Casi toda la carpeta `features/perfil`.
- `AvatarService`.
- `ClientePerfilService`.
- `perfil-cliente.model.ts`.
- Assets de avatares.

### Reescritura/adaptación esperada

- Imports relativos (si tu estructura avanzada difiere).
- Integración de `AuthService.usuario()` si contrato distinto.
- Endpoints de `ClientePerfilService` si el gateway cambió.
- Tests `.spec.ts` (opcionales en primera migración funcional).

### Funcionalidades que podrían romperse

- Carga de perfil por `usuarioId` nulo.
- Guardado de avatar por diferencias en `fotoPerfilUrl`.
- Cambio de contraseña por contrato diferente de `SolicitudCambioPassword`.
- Breadcrumbs/títulos si tu layout avanzado maneja metadata distinta.

### Pruebas mínimas recomendadas

- Prueba de navegación de todas las rutas de Perfil.
- Prueba de integración con backend (GET/PUT perfil).
- Prueba de UI avatar + assets.
- Prueba de sesión expirada y comportamiento de carga.
- Build CI (`npm run build`) sin errores TS/import.

---

## 7) Nota práctica para copiar rápido

Si quieres una migración rápida y segura:

1. Copia `features/perfil` completo.
2. Copia `avatar.service.ts`, `cliente-perfil.service.ts`, `perfil-cliente.model.ts`.
3. Copia `assets/avatares`.
4. Registra `/perfil` en `app.routes.ts`.
5. Ajusta barrels (`core/services` y `core/models`).
6. Corre `npm run build` y corrige imports/contratos restantes.


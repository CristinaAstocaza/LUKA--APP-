# Código completo de HTML y SCSS - Sección Perfil

Documento generado automáticamente desde el repo antiguo.


## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\configuracion\configuracion.html`

```html
<section class="configuracion">
  <header class="configuracion__header">
    <h1>ConfiguraciÃ³n</h1>
    <p>Personaliza tu experiencia y asegura tu cuenta.</p>
  </header>

  <div class="configuracion__grid">
    <article class="configuracion__card">
      <div class="configuracion__card-header">
        <h2>Apariencia</h2>
        <p>Personaliza el estilo de la app.</p>
      </div>

      <div class="configuracion__apariencia-grid">
        <section class="configuracion__panel" [style.--accent-color]="colorPrincipal()">
          <h3>Tema</h3>
          <div class="configuracion__segmentado">
            <button
              type="button"
              [class.is-active]="tema() === 'oscuro'"
              (click)="seleccionarTema('oscuro')"
            >
              <i class="fa-solid fa-moon"></i>
              Oscuro
            </button>
            <button
              type="button"
              [class.is-active]="tema() === 'claro'"
              (click)="seleccionarTema('claro')"
            >
              <i class="fa-solid fa-sun"></i>
              Claro
            </button>
          </div>
        </section>

        <section class="configuracion__panel">
          <h3>Color principal</h3>
          <div class="configuracion__colores">
            @for (color of coloresDisponibles; track color) {
              <button
                type="button"
                [style.background]="color"
                [class.is-active]="colorPrincipal() === color"
                (click)="seleccionarColor(color)"
                aria-label="Seleccionar color"
              ></button>
            }
          </div>

          <h3>Vista previa</h3>
          <div class="configuracion__preview" [style.--previewColor]="colorPrincipal()">
            <p class="configuracion__preview-title">Saldo total</p>
            <p class="configuracion__preview-value">S/ 2,450.00</p>
            <p class="configuracion__preview-caption">Tus finanzas, tu futuro.</p>
          </div>
        </section>
      </div>
    </article>

    <article class="configuracion__card">
      <div class="configuracion__card-header">
        <h2>InformaciÃ³n de la cuenta</h2>
        <p>Administra tu informaciÃ³n personal.</p>
      </div>

      <section class="configuracion__panel" [style.--accent-color]="colorPrincipal()">
        <div class="configuracion__perfil-head">
          <div class="configuracion__perfil-avatar">
            <img [src]="'/assets/avatares/figuras/' + avatarService.avatarConfig().figura + '.png'" [alt]="avatarService.avatarConfig().figura" />
          </div>
          <div>
            <h3>{{ (perfil()?.nombres ?? 'Usuario') + ' ' + (perfil()?.apellidos ?? '') }}</h3>
            <p>{{ perfil()?.telefono || 'Sin telÃ©fono registrado' }}</p>
          </div>
          <button type="button" class="configuracion__cta" (click)="abrirModalEditarPerfil()">Editar perfil</button>
        </div>

        <ul class="configuracion__perfil-lista">
          <li><span>Correo electrÃ³nico</span><strong>{{ authService.usuario()?.nombreUsuario || 'No disponible' }}</strong></li>
          <li><span>Ciudad</span><strong>{{ perfil()?.ciudad || 'No especificado' }}</strong></li>
          <li><span>Fecha de registro</span><strong>{{ perfil()?.fechaCreacion || 'No disponible' }}</strong></li>
        </ul>

        @if (mensajePerfil()) {
          <p class="configuracion__mensaje">{{ mensajePerfil() }}</p>
        }
      </section>
    </article>

    <article class="configuracion__card">
      <div class="configuracion__card-header">
        <h2>Privacidad y legal</h2>
      </div>
      <section class="configuracion__panel">
        <ul class="configuracion__links-lista">
          <li><a href="#" (click)="$event.preventDefault()">PolÃ­tica de privacidad</a></li>
          <li><a href="#" (click)="$event.preventDefault()">TÃ©rminos y condiciones</a></li>
        </ul>
      </section>
    </article>

    <article class="configuracion__card">
      <div class="configuracion__card-header">
        <h2>Soporte</h2>
      </div>
      <section class="configuracion__panel">
        <ul class="configuracion__links-lista">
          <li><a href="#" (click)="$event.preventDefault()">Centro de ayuda</a></li>
          <li><a href="#" (click)="$event.preventDefault()">Contactar soporte</a></li>
          <li><a href="#" (click)="$event.preventDefault()">Reportar un problema</a></li>
        </ul>
      </section>
    </article>

  </div>

  @if (modalEditarPerfilAbierto()) {
    <div class="configuracion__modal-backdrop" (click)="cerrarModalEditarPerfil()">
      <div class="configuracion__modal" [style.--accent-color]="colorPrincipal()" (click)="$event.stopPropagation()">
        <header class="configuracion__modal-header">
          <h3>Editar perfil</h3>
          <button type="button" class="configuracion__close" (click)="cerrarModalEditarPerfil()">Ã—</button>
        </header>

        <div class="configuracion__modal-avatar">
          <div class="configuracion__perfil-avatar configuracion__perfil-avatar--large">
            <img [src]="'/assets/avatares/figuras/' + avatarService.avatarConfig().figura + '.png'" [alt]="avatarService.avatarConfig().figura" />
          </div>
          <p>Avatar actual: {{ avatarService.avatarConfig().figura }}</p>
        </div>

        <div class="configuracion__form-grid">
          <input type="text" placeholder="Nombres" [value]="formNombres()" (input)="formNombres.set($any($event.target).value)" />
          <input type="text" placeholder="Apellidos" [value]="formApellidos()" (input)="formApellidos.set($any($event.target).value)" />
          <input type="text" placeholder="TelÃ©fono" [value]="formTelefono()" (input)="formTelefono.set($any($event.target).value)" />
          <input type="text" placeholder="DirecciÃ³n" [value]="formDireccion()" (input)="formDireccion.set($any($event.target).value)" />
          <input type="text" placeholder="Ciudad" [value]="formCiudad()" (input)="formCiudad.set($any($event.target).value)" />
        </div>

        <button type="button" class="configuracion__cta" [disabled]="guardandoPerfil()" (click)="guardarPerfil()">
          {{ guardandoPerfil() ? 'Guardando...' : 'Guardar cambios' }}
        </button>
      </div>
    </div>
  }
</section>
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\configuracion\configuracion.scss`

```scss
.configuracion {
  width: 100%;
  display: grid;
  gap: 24px;
  padding: 24px;
  background: var(--bg-body);
  color: var(--text-primary);

  &__header {
    h1 {
      margin: 0;
      font-size: 34px;
      color: var(--text-primary);
    }

    p {
      margin: 8px 0 0;
      color: var(--text-secondary);
      font-size: 16px;
    }
  }

  &__grid {
    display: grid;
    gap: 16px;
  }

  &__card {
    background: color-mix(in srgb, var(--bg-card) 94%, var(--color-primary-soft) 6%);
    border: 1px solid color-mix(in srgb, var(--border-color) 78%, var(--color-primary) 22%);
    border-radius: 16px;
    padding: 20px;
    display: grid;
    gap: 16px;
    box-shadow: var(--shadow-sm);
  }

  &__card-header h2 {
    margin: 0;
    font-size: 24px;
    color: var(--text-primary);
  }

  &__card-header p {
    margin: 4px 0 0;
    color: var(--text-secondary);
  }

  &__apariencia-grid {
    display: grid;
    gap: 16px;
  }

  &__panel {
    border: 1px solid color-mix(in srgb, var(--border-color) 84%, var(--color-primary) 16%);
    border-radius: 12px;
    padding: 16px;
    background: color-mix(in srgb, var(--bg-card) 92%, var(--bg-surface-soft) 8%);
    display: grid;
    gap: 12px;
    transition: var(--transition);

    &:hover {
      border-color: color-mix(in srgb, var(--border-color) 60%, var(--accent-color, var(--color-primary)) 40%);
      box-shadow: 0 8px 20px color-mix(in srgb, var(--accent-color, var(--color-primary)) 20%, transparent);
    }

    h3 {
      margin: 0;
      font-size: 18px;
      color: var(--text-primary);
    }
  }

  &__panel--compacto {
    max-width: 360px;
  }

  &__segmentado {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;

    button {
      min-height: 44px;
      border-radius: 10px;
      border: 1px solid var(--border-color);
      background: var(--bg-card);

      &.is-active {
        border-color: var(--accent-color, var(--color-primary));
        background: color-mix(in srgb, var(--accent-color, var(--color-primary)) 14%, #ffffff);
      }
    }
  }

  &__colores {
    display: flex;
    gap: 10px;

    button {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: 2px solid #fff;

      &.is-active { box-shadow: 0 0 0 2px var(--accent-color, var(--color-primary)); }
    }
  }

  &__preview {
    --previewColor: #6d4aff;
    border-radius: 12px;
    padding: 16px;
    color: #fff;
    background: linear-gradient(130deg, color-mix(in srgb, var(--previewColor) 70%, #1f3a8a) 0%, #243b75 100%);
  }

  &__preview-title,
  &__preview-caption,
  &__preview-value { margin: 0; }

  &__preview-value { font-size: 30px; font-weight: 700; }

  &__perfil-head {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 12px;

    p {
      margin: 4px 0 0;
      color: var(--text-secondary);
    }
  }

  &__perfil-avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    border: 3px solid color-mix(in srgb, var(--accent-color, var(--color-primary)) 30%, #ffffff);
    background: #eef2ff;
    display: grid;
    place-items: center;
    overflow: hidden;
    box-shadow: 0 6px 14px rgba(0, 0, 0, 0.08);
    flex-shrink: 0;

    img {
      width: 92%;
      height: 92%;
      object-fit: contain;
    }

    &--large {
      width: 104px;
      height: 104px;
    }
  }

  &__perfil-lista,
  &__links-lista {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 10px;
  }

  &__perfil-lista li {
    display: flex;
    justify-content: space-between;
    border-top: 1px solid #e6edf3;
    padding-top: 10px;
    color: var(--text-secondary);
  }

  &__links-lista a {
    color: var(--text-secondary);
    text-decoration: none;
  }

  &__cta {
    min-height: 44px;
    border: none;
    border-radius: 10px;
    padding: 0 14px;
    background: var(--accent-color, var(--color-primary));
    color: #fff;
    font-weight: 600;
  }

  &__mensaje {
    margin: 0;
    color: #486581;
  }

  &__modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    display: grid;
    place-items: center;
    padding: 16px;
    z-index: 1100;
  }

  &__modal {
    width: min(640px, 100%);
    background: linear-gradient(180deg, var(--bg-card) 0%, color-mix(in srgb, var(--bg-card) 88%, var(--bg-surface-soft) 12%) 100%);
    border-radius: 20px;
    border: 1px solid color-mix(in srgb, var(--border-color) 76%, var(--accent-color, var(--color-primary)) 24%);
    padding: 24px;
    display: grid;
    gap: 16px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.18);
  }

  &__modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 { margin: 0; }
  }

  &__close {
    min-width: 44px;
    min-height: 44px;
    border-radius: 10px;
    border: 1px solid #bcccdc;
    background: #fff;
    font-size: 22px;
  }

  &__modal-avatar {
    display: grid;
    justify-items: center;
    gap: 8px;

    p {
      margin: 0;
      font-size: 13px;
      color: var(--text-secondary);
    }
  }

  &__perfil-head .configuracion__cta {
    margin-left: auto;
  }

  &__form-grid {
    display: grid;
    gap: 10px;

    input {
      min-height: 44px;
      border-radius: 10px;
      border: 1px solid var(--border-color);
      padding: 0 12px;
      background: var(--bg-card);
      color: var(--text-primary);
    }
  }
}

@media (min-width: 1024px) {
  .configuracion {
    &__grid {
      grid-template-columns: 1.2fr 1fr;
      align-items: start;
    }

    &__apariencia-grid {
      grid-template-columns: 1fr 1fr;
    }

    &__card--seguridad {
      grid-column: 1 / -1;
    }
  }
}
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\historial\historial.html`

```html
<p>historial works!</p>
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\historial\historial.scss`

```scss
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-cliente\components\avatar-display\avatar-display.html`

```html
<div
  class="avatar-display"
  [class.avatar-display--loading]="loading()"
  [style.width]="avatarSizePx()"
  [style.height]="avatarSizePx()"
>
  @if (loading()) {
    <div class="avatar-display__skeleton"></div>
  } @else {
    <img
      class="avatar-display__figura"
      [src]="figuraSrc()"
      [alt]="'Figura ' + avatarConfig().figura"
    />

    <img
      class="avatar-display__accesorio"
      [src]="accesorioSrc()"
      [alt]="'Accesorio ' + (avatarConfig().accesorio ?? '')"
    />
  }
</div>

```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-cliente\components\avatar-display\avatar-display.scss`

```scss
.avatar-display {
  position: relative;
  width: 160px;
  height: 160px;
  border-radius: 16px;
  background: #eef3f8;
  border: 1px solid #d7e1eb;
  overflow: hidden;

  &__figura,
  &__accesorio {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    bottom: auto;
    width: 100%;
    height: 100%;
    object-fit: contain;
    object-position: center top;
  }

  &__accesorio {
    z-index: 2;
  }

  &__skeleton {
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #e4ebf3 25%, #f3f7fb 37%, #e4ebf3 63%);
    background-size: 400% 100%;
    animation: avatar-skeleton 1.2s ease-in-out infinite;
  }
}

@keyframes avatar-skeleton {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: 0 0;
  }
}

@media (min-width: 576px) {
  .avatar-display {
    width: 192px;
    height: 192px;
  }
}

```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-cliente\components\avatar-selector\avatar-selector.html`

```html
<section class="avatar-selector">
  <div class="avatar-selector__tabs" role="tablist" aria-label="Selector de avatar">
    <button
      type="button"
      class="avatar-selector__tab"
      [class.avatar-selector__tab--active]="tabActiva() === 'figura'"
      (click)="cambiarTab('figura')"
    >
      Figura
    </button>

    <button
      type="button"
      class="avatar-selector__tab"
      [class.avatar-selector__tab--active]="tabActiva() === 'accesorio'"
      (click)="cambiarTab('accesorio')"
    >
      Accesorio
    </button>
  </div>

  @if (loading()) {
    <div class="avatar-selector__skeleton-grid">
      @for (item of [1, 2, 3, 4]; track item) {
        <div class="avatar-selector__skeleton-item"></div>
      }
    </div>
  } @else {
    @if (tabActiva() === 'figura') {
      <div class="avatar-selector__grid">
        @for (figura of figuras(); track figura) {
          <button
            type="button"
            class="avatar-selector__item"
            [class.avatar-selector__item--selected]="figuraSeleccionada() === figura"
            (click)="seleccionarFigura(figura)"
          >
            <img [src]="getFiguraSrc(figura)" [alt]="figura" />
            <span>{{ figura }}</span>
          </button>
        }
      </div>
    } @else {
      <div class="avatar-selector__grid">
        @for (accesorio of accesorios(); track accesorio) {
          <button
            type="button"
            class="avatar-selector__item"
            [class.avatar-selector__item--selected]="accesorioSeleccionado() === accesorio"
            (click)="seleccionarAccesorio(accesorio)"
          >
            <img [src]="getAccesorioSrc(accesorio)" [alt]="accesorio" />
            <span>{{ accesorio }}</span>
          </button>
        }
      </div>
    }
  }

  @if (mensajeError()) {
    <p class="avatar-selector__error">{{ mensajeError() }}</p>
  }

  <button type="button" class="avatar-selector__save" (click)="guardarCambios()">
    Guardar Cambios
  </button>
</section>

```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-cliente\components\avatar-selector\avatar-selector.scss`

```scss
.avatar-selector {
  display: grid;
  gap: 16px;

  &__tabs {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  &__tab {
    min-height: 44px;
    border: 1px solid #c9d7e6;
    border-radius: 8px;
    background: #ffffff;
    color: #0f2a43;
    font-weight: 600;
    transition: all 0.3s ease;

    &:hover {
      border-color: #9bb3cc;
      box-shadow: 0 0 0 2px #0f2a4310;
    }

    &--active {
      background: #0f2a43;
      border-color: #0f2a43;
      color: #ffffff;
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  &__item {
    min-height: 96px;
    border: 1px solid #d7e1eb;
    border-radius: 12px;
    background: #ffffff;
    display: grid;
    justify-items: center;
    align-content: center;
    gap: 8px;
    padding: 8px;
    color: #0f2a43;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px #0f2a431a;
      border-color: #b7c9dc;
    }

    img {
      width: 56px;
      height: 56px;
      object-fit: contain;
    }

    span {
      font-size: 12px;
      font-weight: 600;
      text-align: center;
    }

    &--selected {
      border-color: #1f6f4a;
      box-shadow: 0 0 0 3px #2f855a44;
      background: #f3fbf7;
    }
  }

  &__skeleton-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  &__skeleton-item {
    min-height: 96px;
    border-radius: 12px;
    background: linear-gradient(90deg, #e4ebf3 25%, #f3f7fb 37%, #e4ebf3 63%);
    background-size: 400% 100%;
    animation: avatar-skeleton 1.2s ease-in-out infinite;
  }

  &__error {
    margin: 0;
    color: #b42318;
    font-size: 14px;
  }

  &__save {
    min-height: 44px;
    border: none;
    border-radius: 8px;
    background: #2f855a;
    color: #ffffff;
    font-weight: 700;
    padding: 0 16px;
    transition: all 0.3s ease;

    &:hover {
      background: #276749;
    }
  }
}

body.theme-dark .avatar-selector {
  &__tab {
    background: #17273a;
    border-color: #3a4d63;
    color: #dbe7f3;

    &:hover {
      border-color: #5d7793;
      box-shadow: 0 0 0 2px #7bb3ff2b;
    }

    &--active {
      background: #5aa0ff;
      border-color: #5aa0ff;
      color: #081521;
      box-shadow: 0 0 0 3px #5aa0ff55;
    }
  }

  &__item {
    background: #162435;
    border-color: #33485f;
    color: #deebf7;

    &:hover {
      border-color: #5d7793;
      box-shadow: 0 6px 14px #00000055;
    }

    &--selected {
      background: #1c3347;
      border-color: #7dd3a6;
      box-shadow: 0 0 0 3px #7dd3a666;
      color: #eafff2;
    }
  }

  &__save {
    background: #2f855a;

    &:hover {
      background: #3aa56f;
    }
  }
}

@keyframes avatar-skeleton {
  0% { background-position: 100% 0; }
  100% { background-position: 0 0; }
}

@media (min-width: 768px) {
  .avatar-selector {
    &__grid,
    &__skeleton-grid {
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 16px;
    }
  }
}

```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-cliente\perfil-cliente.html`

```html

<section class="perfil-cliente">
  <header class="perfil-cliente__page-head card">
    <p>Gestiona tu identidad, seguridad y preferencias de cuenta desde un solo lugar.</p>
  </header>

  <div class="perfil-cliente__layout-fintech">
    <article class="perfil-cliente__hero card">
      @if (loading()) {
        <div class="perfil-cliente__skeleton perfil-cliente__skeleton--hero"></div>
      } @else {
        <div class="perfil-cliente__hero-left">
          <app-avatar-display [avatarConfig]="avatarConfig()" [loading]="false" [size]="180" />
          <button type="button" class="perfil-cliente__edit-avatar" (click)="abrirModalAvatar()">Cambiar avatar</button>
        </div>
        <div class="perfil-cliente__hero-main">
          <h2>{{ nombreMostrado() }}</h2>
          <p>{{ usuarioSesion()?.nombreUsuario || 'Sin correo' }}</p>
          <div class="perfil-cliente__badges">
            <span class="badge badge-success">{{ estadoVerificacion() }}</span>
          </div>
          <div class="perfil-cliente__hero-summary">
            <div class="perfil-cliente__quick-stat">
              <span>Estado de perfil</span>
              <strong>{{ resumenCuenta().estadoPerfil }}</strong>
            </div>
            <div class="perfil-cliente__quick-progress">
              <small>Ãšltima actualizaciÃ³n: {{ resumenCuenta().ultimaActualizacion }}</small>
              <div class="perfil-cliente__meter-track">
                <div class="perfil-cliente__meter-fill" [style.width.%]="perfil()?.datosCompletos ? 100 : 55"></div>
              </div>
            </div>
          </div>
          <blockquote class="perfil-cliente__quote">"Cada decisiÃ³n financiera inteligente construye tu libertad futura."</blockquote>
        </div>

        <div class="perfil-cliente__hero-status">
          <div class="perfil-cliente__status-item">
            <span class="perfil-cliente__status-label">ContraseÃ±a</span>
            <strong class="perfil-cliente__status-value">â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢</strong>
          </div>
          <div class="perfil-cliente__status-item">
            <span class="perfil-cliente__status-label">Miembro desde</span>
            <strong class="perfil-cliente__status-value">{{ miembroDesde() }}</strong>
          </div>
          <div class="perfil-cliente__status-item">
            <span class="perfil-cliente__status-label">Actividad</span>
            <strong class="perfil-cliente__status-value">{{ estadoActividad() }}</strong>
          </div>
        </div>
      }
    </article>

    <div class="perfil-cliente__grid-main">
      <article class="perfil-cliente__section perfil-cliente__section--info card">
        <div class="perfil-cliente__section-head">
          <h2>InformaciÃ³n personal</h2>
        </div>
        @if (loading()) {
          <div class="perfil-cliente__skeleton-grid">
            @for (item of [1,2,3,4,5,6,7]; track item) { <div class="perfil-cliente__skeleton"></div> }
          </div>
        } @else {
          <div class="perfil-cliente__grid-2">
            @for (campo of informacionBasica(); track campo.label) {
              <div class="perfil-cliente__field-group">
                <span class="perfil-cliente__label">{{ campo.label }}</span>
                <div class="perfil-cliente__field perfil-cliente__field--readonly">
                  <span class="perfil-cliente__value">{{ campo.value }}</span>
                </div>
              </div>
            }
            <div class="perfil-cliente__field-group">
              <label class="perfil-cliente__label" for="genero">GÃ©nero</label>
              <div class="perfil-cliente__field">
                <input id="genero" class="perfil-cliente__input" type="text" [value]="camposEditables().genero" (input)="actualizarCampoEditable('genero', $any($event.target).value)"/>
              </div>
            </div>
            <div class="perfil-cliente__field-group">
              <label class="perfil-cliente__label" for="telefono">TelÃ©fono</label>
              <div class="perfil-cliente__field">
                <input id="telefono" class="perfil-cliente__input" type="text" [value]="camposEditables().telefono" (input)="actualizarCampoEditable('telefono', $any($event.target).value)"/>
              </div>
            </div>
            <div class="perfil-cliente__field-group perfil-cliente__field-group--full">
              <label class="perfil-cliente__label" for="ciudad">Ciudad</label>
              <div class="perfil-cliente__field perfil-cliente__field--full">
                <input id="ciudad" class="perfil-cliente__input" type="text" [value]="camposEditables().ciudad" (input)="actualizarCampoEditable('ciudad', $any($event.target).value)"/>
              </div>
            </div>
          </div>
          <button type="button" class="perfil-cliente__action-btn" [disabled]="guardandoPerfil()" (click)="guardarDatosPerfil()">{{ guardandoPerfil() ? 'Guardando...' : 'Guardar informaciÃ³n personal' }}</button>
        }
      </article>

      <div class="perfil-cliente__right-column">
      <article class="perfil-cliente__section perfil-cliente__section--security card">
        <h2>Seguridad de la cuenta</h2>
        <div class="perfil-cliente__grid-1">
          <div class="perfil-cliente__field">
            <label class="perfil-cliente__label" for="passwordActual">ContraseÃ±a actual</label>
            <input id="passwordActual" class="perfil-cliente__input" type="password" [value]="cambioPassword().passwordActual" (input)="actualizarCampoPassword('passwordActual', $any($event.target).value)"/>
          </div>
          <div class="perfil-cliente__field">
            <label class="perfil-cliente__label" for="nuevoPassword">Nueva contraseÃ±a</label>
            <input id="nuevoPassword" class="perfil-cliente__input" type="password" [value]="cambioPassword().nuevoPassword" (input)="actualizarCampoPassword('nuevoPassword', $any($event.target).value)"/>
          </div>
          <div class="perfil-cliente__security-meter">
            <span>Seguridad: {{ fortalezaPassword().label }}</span>
            <div class="perfil-cliente__meter-track">
              <div class="perfil-cliente__meter-fill" [style.width.%]="fortalezaPassword().percent"></div>
            </div>
          </div>
          <div class="perfil-cliente__field">
            <label class="perfil-cliente__label" for="confirmarPassword">Confirmar nueva contraseÃ±a</label>
            <input id="confirmarPassword" class="perfil-cliente__input" type="password" [value]="cambioPassword().confirmarPassword" (input)="actualizarCampoPassword('confirmarPassword', $any($event.target).value)"/>
          </div>
        </div>
        <button type="button" class="perfil-cliente__action-btn" [disabled]="guardandoPassword()" (click)="guardarPassword()">{{ guardandoPassword() ? 'Guardando...' : 'Actualizar contraseÃ±a' }}</button>
      </article>

      <article class="perfil-cliente__section perfil-cliente__section--timeline card">
      <div class="perfil-cliente__section-head">
        <h2>Actividad reciente</h2>
      </div>
      <div class="perfil-cliente__timeline">
        @for (item of actividadesRecientes(); track item.titulo) {
          <div class="perfil-cliente__timeline-item">
            <strong>{{ item.titulo }}</strong>
            <span>{{ item.detalle }}</span>
            <small>{{ item.fecha }}</small>
          </div>
        }
      </div>
    </article>
      </div>
    </div>

    <article class="perfil-cliente__section perfil-cliente__section--danger card">
      <div class="perfil-cliente__section-head">
        <h2>Zona de cuenta</h2>
      </div>
      <button type="button" class="perfil-cliente__danger-btn" (click)="ejecutarOpcionRapida('eliminar')">
        Eliminar cuenta
      </button>
    </article>
  </div>

  @if (modalAbierto()) {
    <div class="perfil-cliente__modal-backdrop" (click)="cerrarModalAvatar()">
      <div class="perfil-cliente__modal" (click)="$event.stopPropagation()">
        <div class="perfil-cliente__modal-header">
          <h2>Editar Avatar</h2>
          <button type="button" class="perfil-cliente__close" (click)="cerrarModalAvatar()">Ã—</button>
        </div>

        <div class="perfil-cliente__modal-layout">
          <section class="perfil-cliente__modal-preview">
            <h3>Vista previa</h3>
            <app-avatar-display [avatarConfig]="avatarConfigActual()" [loading]="false" [size]="300" />
          </section>

          <section class="perfil-cliente__modal-selector">
            <app-avatar-selector
              [avatarConfig]="avatarConfig()"
              [loading]="loading()"
              (preview)="actualizarPreviewAvatar($event)"
              (save)="guardarAvatar($event)"
            />
          </section>
        </div>
      </div>
    </div>
  }

  @if (mensajeExito()) {
    <p class="perfil-cliente__success" role="status">{{ mensajeExito() }}</p>
  }
</section>
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-cliente\perfil-cliente.scss`

```scss
.perfil-cliente {
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  gap: 20px;

  &__page-head {
    padding: 20px;
    display: grid;
    gap: 6px;

    h1 {
      margin: 0;
      font-size: 2rem;
      font-family: var(--font-heading);
      color: var(--text-primary);
    }

    p {
      margin: 0;
      color: var(--text-secondary);
    }
  }

  &__layout,
  &__layout-fintech {
    display: grid;
    gap: 16px;
  }

  &__grid-main {
    display: grid;
    gap: 16px;
  }

  &__hero,
  &__section,
  &__side {
    padding: 20px;
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-color);
    background: var(--bg-card);
    box-shadow: var(--shadow-sm);
    transition: transform .35s cubic-bezier(.2,.8,.2,1), box-shadow .35s ease, border-color .35s ease;

    &:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: color-mix(in srgb, var(--color-primary) 35%, var(--border-color));
    }
  }

  &__hero {
    display: grid;
    gap: 20px;
    grid-template-columns: 1fr;
    border-radius: var(--radius-xl);
    background: linear-gradient(
      135deg,
      color-mix(in srgb, var(--color-primary) 18%, var(--bg-card)),
      color-mix(in srgb, var(--color-primary-light) 10%, var(--bg-card))
    );
    animation: hero-fade-in .6s ease both;
  }

  &__hero-status {
    display: grid;
    gap: 8px;
    background: var(--bg-surface-soft);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    padding: 12px;
    animation: float-soft 4s ease-in-out infinite;
  }

  &__status-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  &__status-label {
    color: var(--text-secondary);
    font-size: 0.86rem;
    font-weight: 600;
  }

  &__status-value {
    color: var(--text-primary);
    font-size: 0.92rem;
    font-weight: 700;
  }

  &__hero-left {
    display: grid;
    justify-items: center;
    gap: 14px;
  }

  &__hero-main {
    display: grid;
    gap: 12px;

    h2 {
      margin: 0;
      color: var(--text-primary);
      font-family: var(--font-heading);
    }

    p {
      margin: 0;
      color: var(--text-secondary);
    }
  }

  &__hero-summary {
    display: grid;
    gap: 10px;
    padding: 10px 12px;
    border-radius: var(--radius-md);
    border: 1px solid color-mix(in srgb, var(--color-primary) 22%, var(--border-color));
    background: color-mix(in srgb, var(--bg-card) 70%, transparent);
    backdrop-filter: blur(8px);
  }

  &__quick-stat {
    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      color: var(--text-secondary);
      font-size: .82rem;
    }

    strong {
      color: var(--text-primary);
      font-size: 1rem;
      font-family: var(--font-heading);
    }
  }

  &__quick-progress {
    display: grid;
    gap: 6px;

    small {
      color: var(--text-secondary);
      font-weight: 700;
      font-size: .75rem;
    }
  }

  &__badges {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__quote {
    margin: 0;
    border-left: 3px solid var(--color-primary);
    padding-left: 10px;
    color: var(--text-secondary);
    font-style: italic;
    animation: pulse-soft 3s ease-in-out infinite;
  }

  &__right-column {
    display: grid;
    gap: 16px;
    align-content: start;
  }

  &__side {
    display: grid;
    gap: 12px;
  }

  &__mini-card {
    display: grid;
    gap: 8px;
    background: var(--bg-surface-soft);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    padding: 14px;

    h3,
    p {
      margin: 0;
      color: var(--text-primary);
    }
  }

  &__activity-item {
    display: grid;
    gap: 2px;

    strong {
      color: var(--text-primary);
      font-size: 0.92rem;
    }

    span,
    small {
      color: var(--text-secondary);
      font-size: 0.82rem;
    }
  }

  &__section {
    display: grid;
    gap: 14px;
    border-radius: var(--radius-xl);

    h2 {
      margin: 0;
      color: var(--text-primary);
      font-size: 1.2rem;
      font-family: var(--font-heading);
    }
  }

  &__section--security {
    max-height: fit-content;

    .perfil-cliente__field {
      min-height: 58px;
    }
  }

  &__section-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }

  &__section--timeline {
    margin-top: 2px;
  }

  &__section--widgets {
    background: linear-gradient(
      145deg,
      color-mix(in srgb, var(--bg-card) 80%, var(--bg-surface-soft)),
      color-mix(in srgb, var(--bg-card) 92%, var(--color-primary-soft))
    );
  }

  &__widgets-grid {
    display: grid;
    gap: 10px;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  &__widget-card {
    display: grid;
    gap: 8px;
    padding: 10px;
    border-radius: var(--radius-md);
    border: 1px solid var(--border-color);
    background: color-mix(in srgb, var(--bg-card) 75%, transparent);
    backdrop-filter: blur(6px);
    transition: var(--transition);

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-sm);
    }

    p {
      margin: 0;
      color: var(--text-primary);
      font-weight: 800;
      font-family: var(--font-heading);
    }
  }

  &__widget-head {
    display: flex;
    align-items: center;
    gap: 8px;

    i {
      color: var(--color-primary);
      font-size: .9rem;
    }

    strong {
      color: var(--text-secondary);
      font-size: .78rem;
    }
  }

  &__tip-card {
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 8px;
    border-radius: var(--radius-md);
    border: 1px solid color-mix(in srgb, var(--color-info) 25%, var(--border-color));
    background: color-mix(in srgb, var(--color-info-soft) 55%, var(--bg-card));
    padding: 10px;

    i {
      color: var(--color-info);
    }

    span {
      color: var(--text-secondary);
      font-size: .82rem;
      font-weight: 700;
    }
  }

  &__grid-2 {
    display: grid;
    gap: 12px;
    grid-template-columns: 1fr;
  }

  &__grid-1 {
    display: grid;
    gap: 12px;
    grid-template-columns: 1fr;
  }

  &__field {
    display: grid;
    gap: 8px;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--bg-card);
    padding: 10px 12px;
    min-height: 64px;

    &--readonly {
      background: var(--bg-surface-soft);
    }

    &--full {
      grid-column: 1 / -1;
    }
  }

  &__field-group {
    display: grid;
    gap: 8px;

    &--full {
      grid-column: 1 / -1;
    }
  }

  &__label {
    color: var(--text-secondary);
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  &__value {
    color: var(--text-primary);
    font-weight: 600;
    font-size: 0.92rem;
  }

  &__input {
    min-height: 38px;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    padding: 0 10px;
    background: var(--bg-card);
    color: var(--text-primary);

    &:focus {
      outline: none;
      border-color: var(--color-primary);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 20%, transparent);
    }
  }

  &__action-btn,
  &__quick-btn,
  &__edit-avatar {
    min-height: 46px;
    border-radius: var(--radius-sm);
    border: 1px solid var(--color-primary);
    background: var(--color-primary);
    color: #fff;
    font-weight: 700;
    padding: 0 14px;
    transition: var(--transition-bounce);

    &:hover {
      background: var(--color-primary-dark);
      border-color: var(--color-primary-dark);
      transform: translateY(-2px) scale(1.01);
    }

    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  }

  &__danger-btn {
    min-height: 46px;
    border-radius: var(--radius-sm);
    border: 1px solid var(--color-danger);
    background: color-mix(in srgb, var(--color-danger) 10%, var(--bg-card));
    color: var(--color-danger);
    font-weight: 800;
    padding: 0 16px;
    transition: var(--transition-bounce);

    &:hover {
      background: var(--color-danger-soft);
      transform: translateY(-2px) scale(1.01);
      box-shadow: 0 10px 24px color-mix(in srgb, var(--color-danger) 18%, transparent);
    }
  }

  &__quick-actions {
    display: grid;
    gap: 10px;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }

  &__quick-btn {
    text-align: left;
    background: var(--bg-card);
    color: var(--text-primary);
    border-color: var(--border-color);

    &:hover {
      background: var(--bg-surface-soft);
      border-color: var(--color-primary-light);
    }

    &--danger {
      border-color: var(--color-danger);
      color: var(--color-danger);

      &:hover {
        background: var(--color-danger-soft);
      }
    }
  }

  &__security-meter {
    display: grid;
    gap: 8px;
    align-content: center;

    span {
      color: var(--text-secondary);
      font-size: 0.9rem;
      font-weight: 700;
    }
  }

  &__meter-track {
    height: 10px;
    border-radius: var(--radius-full);
    background: var(--bg-surface-soft);
    border: 1px solid var(--border-color);
    overflow: hidden;
  }

  &__meter-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--color-warning), var(--color-success));
    transition: var(--transition);
  }

  &__success {
    position: sticky;
    bottom: 12px;
    margin: 0;
    justify-self: end;
    color: var(--color-success);
    background: var(--color-success-soft);
    border: 1px solid color-mix(in srgb, var(--color-success) 35%, transparent);
    border-radius: var(--radius-sm);
    padding: 10px 14px;
    font-size: 0.9rem;
    box-shadow: var(--shadow-md);
  }

  &__timeline {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  &__timeline-item {
    display: grid;
    gap: 4px;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--bg-surface-soft);
    padding: 12px;
    min-height: 116px;
    transition: var(--transition);

    &:hover {
      transform: translateY(-3px);
      border-color: color-mix(in srgb, var(--color-primary) 30%, var(--border-color));
      box-shadow: var(--shadow-sm);
    }

    strong {
      color: var(--text-primary);
      font-size: 0.93rem;
    }

    span,
    small {
      color: var(--text-secondary);
      font-size: 0.82rem;
    }
  }

  &__skeleton-grid {
    display: grid;
    gap: 10px;
  }

  &__skeleton {
    min-height: 56px;
    border-radius: var(--radius-sm);
    background: linear-gradient(90deg, var(--bg-surface-soft) 25%, var(--bg-card) 37%, var(--bg-surface-soft) 63%);
    background-size: 400% 100%;
    animation: perfil-skeleton 1.2s ease-in-out infinite;
  }

  &__skeleton--hero {
    min-height: 220px;
  }

  &__modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.65);
    display: grid;
    place-items: center;
    padding: 16px;
    z-index: 1000;
  }

  &__modal {
    width: min(980px, 100%);
    max-height: 92vh;
    overflow: auto;
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-color);
    background: var(--bg-card);
    padding: 24px;
  }

  &__modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  &__close {
    min-width: 40px;
    min-height: 40px;
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-color);
    background: var(--bg-card);
    color: var(--text-primary);
    font-size: 22px;
  }

  &__modal-layout {
    display: grid;
    gap: 16px;
  }

  &__modal-preview,
  &__modal-selector {
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    background: var(--bg-surface-soft);
    padding: 14px;
  }
}

body.theme-dark .perfil-cliente {
  &__hero {
    background: linear-gradient(
      135deg,
      color-mix(in srgb, var(--color-primary) 26%, var(--bg-card)),
      color-mix(in srgb, var(--color-primary-dark) 18%, var(--bg-card))
    );
  }
}

@keyframes perfil-skeleton {
  0% { background-position: 100% 0; }
  100% { background-position: 0 0; }
}

@keyframes hero-fade-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float-soft {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@keyframes pulse-soft {
  0%, 100% { opacity: .95; }
  50% { opacity: 1; }
}

@media (min-width: 1024px) {
  .perfil-cliente {
    &__layout,
    &__layout-fintech {
      grid-template-columns: 1fr;
      align-items: start;
    }

    &__grid-main {
      grid-template-columns: 1.08fr .92fr;
      align-items: stretch;
    }

    &__section--widgets {
      grid-column: 1;
    }

    &__hero,
    &__section--danger {
      grid-column: 1 / -1;
    }

    &__hero {
      grid-template-columns: 220px minmax(320px, 1fr) 320px;
      align-items: center;
    }

    &__grid-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    &__modal-layout {
      grid-template-columns: 320px 1fr;
    }

    &__timeline {
      grid-template-columns: 1fr;
    }

    &__widgets-grid {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }
}

@media (max-width: 900px) {
  .perfil-cliente {
    padding: 16px;

    &__hero {
      grid-template-columns: 1fr;
      text-align: center;
    }

    &__hero-main {
      justify-items: center;
    }

    &__hero-status {
      width: 100%;
    }

    &__status-item {
      padding: 6px 2px;
      border-bottom: 1px solid var(--border-color);

      &:last-child {
        border-bottom: 0;
      }
    }

    &__grid-main,
    &__grid-2,
    &__timeline {
      grid-template-columns: 1fr;
    }

    &__widgets-grid {
      grid-template-columns: 1fr;
    }

    &__right-column {
      grid-template-columns: 1fr;
    }
  }
}
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-financiero\perfil-financiero.html`

```html
<p>perfil-financiero works!</p>
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-financiero\perfil-financiero.scss`

```scss
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-layout\perfil-layout.html`

```html
<div class="perfil-content">

    <router-outlet />

</div>
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\perfil-layout\perfil-layout.scss`

```scss
.perfil-wrapper {
  display: flex;
  height: 100%;
  gap: 0;
}

.perfil-sidebar {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid #e0e0e0;
  padding: 1.5rem 0;

  .sidebar-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #999;
    padding: 0 1rem;
    margin-bottom: 8px;
  }

  nav {
    display: flex;
    flex-direction: column;

    a {
      padding: 10px 1rem;
      font-size: 14px;
      color: #555;
      text-decoration: none;
      border-left: 2px solid transparent;
      transition: all 0.2s;

      &:hover { background: #f5f5f5; }

      &.active {
        color: #5a52d5;
        background: #eeecfd;
        border-left-color: #5a52d5;
        font-weight: 500;
      }
    }
  }
}

.perfil-content {
  flex: 1;
  padding: 1.5rem 2rem;
  overflow-y: auto;
}
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\transacciones\transacciones.html`

```html
<p>transacciones works!</p>
```

## `D:\CURSOS\7MO\INTEGRADOR FRONTEND\luka-frontend\frontend\src\app\features\perfil\transacciones\transacciones.scss`

```scss
```

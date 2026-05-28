import { Component, computed, inject, signal } from '@angular/core';
import { AvatarConfig, AvatarService } from '../../../core/services/avatar.service';
import { AvatarDisplay } from './components/avatar-display/avatar-display';
import { AvatarSelector } from './components/avatar-selector/avatar-selector';
import { ClientePerfilService } from '../../../core/services';
import { AuthService } from '../../../core/services';
import { RespuestaDatosPersonales } from '../../../core/models';
import { SolicitudCambioPassword } from '../../../core/models/auth/user.model';

interface CampoBasico {
  label: string;
  value: string;
}

interface ActividadReciente {
  titulo: string;
  detalle: string;
  fecha: string;
}

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [AvatarDisplay, AvatarSelector],
  templateUrl: './perfil-cliente.html',
  styleUrl: './perfil-cliente.scss',
})
export class PerfilCliente {


  // Servicios principales 
  private readonly avatarService = inject(AvatarService);
  private readonly clientePerfilService = inject(ClientePerfilService);
  private readonly authService = inject(AuthService);


  //Componentes temporales 
  loading = signal(true);
  modalAbierto = signal(false);
  mensajeExito = signal('');
  perfil = signal<RespuestaDatosPersonales | null>(null);
  guardandoPerfil = signal(false);
  guardandoPassword = signal(false);

  readonly camposEditables = signal({
    genero: '',
    telefono: '',
    ciudad: '',
  });

  readonly cambioPassword = signal<SolicitudCambioPassword>({
    passwordActual: '',
    nuevoPassword: '',
    confirmarPassword: '',
  });

  readonly avatarConfig = computed(() => this.avatarService.avatarConfig());
  readonly usuarioSesion = computed(() => this.authService.usuario());

  readonly actividadesRecientes = computed<ActividadReciente[]>(() => {
    const perfil = this.perfil();
    const actividades: ActividadReciente[] = [];

    if (perfil?.fechaActualizacion) {
      actividades.push({
        titulo: 'Actualización de perfil',
        detalle: 'Se registró una actualización en tus datos personales.',
        fecha: this.formatearFecha(perfil.fechaActualizacion),
      });
    }

    if (perfil?.fechaCreacion) {
      actividades.push({
        titulo: 'Registro de cuenta',
        detalle: 'Tu cuenta fue creada correctamente en la plataforma.',
        fecha: this.formatearFecha(perfil.fechaCreacion),
      });
    }

    if (this.usuarioSesion()) {
      actividades.push({
        titulo: 'Sesión activa',
        detalle: 'Tu sesión está activa en este dispositivo.',
        fecha: 'Ahora',
      });
    }

    return actividades;
  });


  // Combinación de datos del perfil backend + correo de sesión.
  readonly informacionBasica = computed<CampoBasico[]>(() => {
    const perfil = this.perfil();
    const usuario = this.usuarioSesion();

    const miembroDesde = this.formatearFecha(perfil?.fechaCreacion);

    return [
      { label: 'Nombres', value: this.formatearValor(perfil?.nombres) },
      { label: 'DNI', value: this.formatearValor(perfil?.dni) },
      { label: 'Edad', value: this.formatearValor(perfil?.edad) },
      { label: 'Correo', value: this.formatearValor(usuario?.nombreUsuario) },
      {
        label: 'Miembro desde',
        value: this.formatearValor(miembroDesde),
      },
    ];
  });

  readonly nombreMostrado = computed(() => {
    const perfil = this.perfil();
    return `${perfil?.nombres ?? ''} ${perfil?.apellidos ?? ''}`.trim() || 'Usuario Luka';
  });

  readonly estadoVerificacion = computed(() => this.perfil()?.datosCompletos ? 'Cuenta verificada' : 'Verificación pendiente');

  readonly miembroDesde = computed(() => this.formatearFecha(this.perfil()?.fechaCreacion));

  readonly estadoActividad = computed(() => this.usuarioSesion() ? 'Activo ahora' : 'Sin sesión');

  readonly resumenCuenta = computed(() => {
    const perfil = this.perfil();
    return {
      estadoPerfil: perfil?.datosCompletos ? 'Perfil completo' : 'Perfil pendiente',
      ultimaActualizacion: this.formatearFecha(perfil?.fechaActualizacion),
    };
  });

  readonly fortalezaPassword = computed(() => {
    const value = this.cambioPassword().nuevoPassword;
    let score = 0;
    if (value.length >= 8) score++;
    if (/[A-Z]/.test(value)) score++;
    if (/[0-9]/.test(value)) score++;
    if (/[^A-Za-z0-9]/.test(value)) score++;

    if (score <= 1) return { label: 'Débil', percent: 25 };
    if (score === 2) return { label: 'Media', percent: 50 };
    if (score === 3) return { label: 'Buena', percent: 75 };
    return { label: 'Fuerte', percent: 100 };
  });

  constructor() {
    this.cargarDatosPerfil();
  }

  guardarAvatar(config: AvatarConfig): void {
    const perfilActual = this.perfil();
    const usuarioId = this.authService.usuario()?.id;

    if (!perfilActual || !usuarioId) {
      return;
    }

    const fotoPerfilUrl = this.construirAvatarPersistible(config);

    const payload = {
      dni: perfilActual.dni,
      nombres: perfilActual.nombres,
      apellidos: perfilActual.apellidos,
      genero: perfilActual.genero,
      edad: perfilActual.edad,
      telefono: perfilActual.telefono,
      fotoPerfilUrl,
      direccion: perfilActual.direccion,
      ciudad: perfilActual.ciudad,
    };

    this.clientePerfilService.actualizarPerfil(usuarioId, payload).subscribe({
      next: (perfilActualizado) => {
        this.avatarService.setAvatar(config);
        this.perfil.set(perfilActualizado);
        this.cerrarModalAvatar();
        this.mensajeExito.set('Avatar guardado en backend correctamente.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
      error: () => {
        this.mensajeExito.set('No se pudo guardar el avatar en backend.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
    });
  }

  abrirModalAvatar(): void {
    this.modalAbierto.set(true);
  }

  cerrarModalAvatar(): void {
    this.modalAbierto.set(false);
  }

  actualizarCampoEditable(campo: 'genero' | 'telefono' | 'ciudad', valor: string): void {
    this.camposEditables.update((prev) => ({ ...prev, [campo]: valor }));
  }

  actualizarCampoPassword(campo: keyof SolicitudCambioPassword, valor: string): void {
    this.cambioPassword.update((prev) => ({ ...prev, [campo]: valor }));
  }

  guardarDatosPerfil(): void {
    const perfilActual = this.perfil();
    const usuarioId = this.authService.usuario()?.id;
    if (!perfilActual || !usuarioId) return;

    const editables = this.camposEditables();
    this.guardandoPerfil.set(true);

    this.clientePerfilService.actualizarPerfil(usuarioId, {
      dni: perfilActual.dni,
      nombres: perfilActual.nombres,
      apellidos: perfilActual.apellidos,
      genero: editables.genero,
      edad: perfilActual.edad,
      telefono: editables.telefono,
      fotoPerfilUrl: perfilActual.fotoPerfilUrl,
      direccion: perfilActual.direccion,
      ciudad: editables.ciudad,
    }).subscribe({
      next: (perfilActualizado) => {
        this.perfil.set(perfilActualizado);
        this.camposEditables.set({
          genero: perfilActualizado.genero ?? '',
          telefono: perfilActualizado.telefono ?? '',
          ciudad: perfilActualizado.ciudad ?? '',
        });
        this.guardandoPerfil.set(false);
        this.mensajeExito.set('Perfil actualizado correctamente.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
      error: () => {
        this.guardandoPerfil.set(false);
        this.mensajeExito.set('No se pudo actualizar el perfil.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
    });
  }

  guardarPassword(): void {
    const payload = this.cambioPassword();
    if (!payload.passwordActual || !payload.nuevoPassword || !payload.confirmarPassword) {
      this.mensajeExito.set('Completa todos los campos de contraseña.');
      setTimeout(() => this.mensajeExito.set(''), 2500);
      return;
    }

    this.guardandoPassword.set(true);
    this.authService.cambiarPassword(payload).subscribe({
      next: () => {
        this.guardandoPassword.set(false);
        this.cambioPassword.set({ passwordActual: '', nuevoPassword: '', confirmarPassword: '' });
        this.mensajeExito.set('Contraseña actualizada correctamente.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
      error: () => {
        this.guardandoPassword.set(false);
        this.mensajeExito.set('No se pudo actualizar la contraseña.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
    });
  }

  ejecutarOpcionRapida(accion: 'correo' | 'dos-pasos' | 'eliminar'): void {
    const mensajes = {
      correo: 'Próximamente: flujo para cambio de correo.',
      'dos-pasos': 'Próximamente: activación de verificación en 2 pasos.',
      eliminar: 'Próximamente: flujo seguro de eliminación de cuenta.',
    } as const;

    this.mensajeExito.set(mensajes[accion]);
    setTimeout(() => this.mensajeExito.set(''), 2500);
  }

  private cargarDatosPerfil(): void {
    // Si no hay sesión válida, no intenta consultar backend.
    const usuarioId = this.authService.usuario()?.id;

    if (!usuarioId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.clientePerfilService.obtenerPerfil(usuarioId).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.camposEditables.set({
          genero: perfil.genero ?? '',
          telefono: perfil.telefono ?? '',
          ciudad: perfil.ciudad ?? '',
        });
        const avatarBackend = this.extraerAvatarDesdeFotoPerfilUrl(perfil.fotoPerfilUrl);
        if (avatarBackend) {
          this.avatarService.setAvatar(avatarBackend);
        }
        this.loading.set(false);
      },
      error: () => {
        this.perfil.set(null);
        this.loading.set(false);
      },
    });
  }

  private construirAvatarPersistible(config: AvatarConfig): string {
    return `avatar-config:${encodeURIComponent(config.figura)}|${encodeURIComponent(config.accesorio ?? '')}`;
  }

  private extraerAvatarDesdeFotoPerfilUrl(fotoPerfilUrl?: string): AvatarConfig | null {
    if (!fotoPerfilUrl || !fotoPerfilUrl.startsWith('avatar-config:')) {
      return null;
    }

    const raw = fotoPerfilUrl.replace('avatar-config:', '');
    const [figuraRaw, accesorioRaw] = raw.split('|');
    const figura = decodeURIComponent(figuraRaw ?? '');
    const accesorio = decodeURIComponent(accesorioRaw ?? '');

    if (!figura) {
      return null;
    }

    return {
      figura,
      accesorio,
    };
  }

  private formatearValor(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return 'No especificado';
    }
    return String(value);
  }

  private formatearFecha(fecha?: string): string {
    // Formato amigable para UI en locale es-PE.
    if (!fecha) {
      return 'No especificado';
    }

    const fechaParseada = new Date(fecha);
    if (Number.isNaN(fechaParseada.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    }).format(fechaParseada);
  }
}

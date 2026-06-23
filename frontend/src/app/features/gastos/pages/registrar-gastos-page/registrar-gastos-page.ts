import { Component, computed, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { IaService } from '../../../../core/services/ia.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { MetodoPago, TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';

@Component({
  selector: 'app-registrar-gastos-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './registrar-gastos-page.html',
  styleUrl: '../gastos-page/gastos-page.scss'
})
export class RegistrarGastosPage implements OnDestroy {
  private readonly transaccionesService = inject(Transacciones);
  private readonly authService = inject(AuthService);
  private readonly financieroService = inject(FinancieroService);
  private readonly eventBus = inject(AppEventBus);
  private readonly stateService = inject(GastosStateService);
  private readonly iaService = inject(IaService);
  private readonly router = inject(Router);
  private readonly pendientesStorageKey = 'luka:gastos:pendientes-locales';

  readonly categoria = signal('');
  readonly monto = signal('');
  readonly nombreGasto = signal('');
  readonly descripcion = signal('');
  readonly fecha = signal('');
  readonly metodoPago = signal<MetodoPago>('DIGITAL');
  readonly registrarComoPendiente = signal(false);
  readonly etiquetas = signal<string[]>([]);
  readonly nuevaEtiqueta = signal('');
  readonly errores = signal<Record<string, string>>({});
  readonly mensajeFormulario = signal('');
  readonly guardandoGasto = signal(false);
  readonly sugerenciasIa = signal<string[]>([]);
  readonly clasificandoIa = signal(false);

  readonly intentosIaRestantes = computed(() => this.iaService.clasificacionesRestantes());
  readonly intentosIaMaximos = computed(() => this.iaService.clasificacionesMaximas());
  readonly puedeSugerirCategoriaIa = computed(() =>
    this.descripcion().trim().length >= 4 && !this.clasificandoIa() && this.intentosIaRestantes() > 0
  );

  get categoriasDisponibles(): any[] {
    return this.stateService.categorias().length > 0
      ? this.stateService.categorias()
      : [
          { id: 'alimentos', nombre: 'Alimentos' },
          { id: 'transporte', nombre: 'Transporte' },
          { id: 'servicios', nombre: 'Servicios' },
          { id: 'hogar', nombre: 'Hogar' },
          { id: 'otros', nombre: 'Otros' }
        ];
  }

  get categoriasConCrear(): any[] {
    return [
      ...this.categoriasDisponibles,
      { id: 'CREAR_NUEVA', nombre: '＋ Crear nueva categoría...' }
    ];
  }

  readonly metodosPagoDisponibles: Array<{ id: MetodoPago; nombre: string }> = [
    { id: 'DIGITAL', nombre: 'Digital (Yape/Plin)' },
    { id: 'TARJETA', nombre: 'Tarjeta' },
    { id: 'TRANSFERENCIA', nombre: 'Transferencia' },
    { id: 'EFECTIVO', nombre: 'Efectivo' },
  ];

  readonly pendientesMock = signal<Array<{
    id: string;
    nombre: string;
    frecuencia: 'MENSUAL' | 'SEMANAL' | 'QUINCENAL';
    fechaVencimiento: string;
    monto: number;
    vencePronto: boolean;
    metodoPago: MetodoPago;
    categoriaIcono: string;
  }>>([]);

  private txSub?: { unsubscribe: () => void };

  constructor() {
    this.cargarPendientesLocales();
    this.stateService.cargarDatos();
    this.fecha.set(new Date().toISOString().slice(0, 10));
  }

  ngOnDestroy(): void {
    this.txSub?.unsubscribe();
  }

  cancelar(): void {
    this.router.navigate(['/gastos']);
  }

  guardarGasto(): void {
    const errores = this.validarFormulario();
    this.errores.set(errores);
    this.mensajeFormulario.set('');

    if (Object.keys(errores).length > 0) return;

    this.guardandoGasto.set(true);

    const getLocalIsoString = (dateString: string): string => {
      let localDate = new Date();
      if (dateString) {
        const parts = dateString.split('-');
        if (parts.length === 3) {
          localDate = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        } else {
          localDate = new Date(dateString);
        }
      }
      const now = new Date();
      localDate.setHours(now.getHours(), now.getMinutes(), now.getSeconds());
      const tzOffset = localDate.getTimezoneOffset() * 60000;
      return new Date(localDate.getTime() - tzOffset).toISOString().slice(0, 19);
    };

    if (this.registrarComoPendiente()) {
      const fechaVencimiento = this.fecha()
        ? new Date(`${this.fecha()}T00:00:00`)
        : new Date();
      this.pendientesMock.update((items) => [
        {
          id: `pend-local-${Date.now()}`,
          nombre: this.nombreGasto().trim(),
          frecuencia: 'MENSUAL',
          fechaVencimiento: fechaVencimiento.toLocaleDateString('es-PE', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
          }),
          monto: Number(this.monto()),
          vencePronto: this.venceEnDias(fechaVencimiento, 3),
          metodoPago: this.metodoPago(),
          categoriaIcono: this.iconoCategoria(
            this.categoriasDisponibles.find((cat) => cat.id === this.categoria())?.nombre ?? 'otros'
          )
        },
        ...items
      ]);
      this.guardarPendientesLocales(this.pendientesMock());
      this.guardandoGasto.set(false);
      this.router.navigate(['/gastos']);
      return;
    }

    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      this.mensajeFormulario.set('No se encontró sesión activa.');
      this.guardandoGasto.set(false);
      return;
    }

    const request: TransaccionRequestDTO = {
      usuarioId,
      nombreCliente: this.authService.usuario()?.nombreUsuario ?? 'Cliente',
      monto: Number(this.monto()),
      tipo: 'GASTO',
      categoriaId: this.categoria(),
      fechaTransaccion: getLocalIsoString(this.fecha()),
      metodoPago: this.metodoPago(),
      notas: `${this.nombreGasto().trim()}|${this.descripcion().trim()}`,
      descripcion: this.descripcion().trim(),
      etiquetas: this.etiquetas().join(',')
    };

    this.txSub = this.transaccionesService.registrar(request).subscribe({
      next: () => {
        this.guardandoGasto.set(false);
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        this.router.navigate(['/gastos']);
      },
      error: () => {
        this.guardandoGasto.set(false);
        this.mensajeFormulario.set('No se pudo registrar el gasto.');
      },
    });
  }

  clasificarConIa(): void {
    const d = this.descripcion().trim();
    if (!d || d.length < 4) {
      this.sugerenciasIa.set([]);
      return;
    }
    if (this.clasificandoIa() || this.intentosIaRestantes() <= 0) return;
    this.clasificandoIa.set(true);

    this.iaService.getClasificarTransaccion({
      id_temporal: 'nuevo-gasto',
      tipo_movimiento: 'GASTO',
      descripcion: d,
      etiquetas: this.etiquetas().join(',')
    }).subscribe({
      next: (res) => {
        this.clasificandoIa.set(false);
        if (res.datos) {
          const categorias = res.datos.consejo?.categorias_sugeridas || res.datos.categorias_sugeridas || res.datos.sugerencias;
          if (categorias) this.sugerenciasIa.set(categorias);
        }
      },
      error: () => {
        this.clasificandoIa.set(false);
        const matched = ['Alimentos', 'Transporte', 'Servicios', 'Hogar', 'Salud', 'Educación', 'Entretenimiento'].filter((c) =>
          c.toLowerCase().includes(d.toLowerCase())
        );
        this.sugerenciasIa.set(matched.length > 0 ? matched : ['Otros Gastos']);
      }
    });
  }

  agregarEtiqueta(): void {
    const raw = this.nuevaEtiqueta().trim();
    if (!raw) return;
    const tag = raw.split(' ')[0];
    if (!this.etiquetas().includes(tag)) {
      this.etiquetas.update((tags) => [...tags, tag]);
    }
    this.nuevaEtiqueta.set('');
  }

  eliminarEtiqueta(tag: string): void {
    this.etiquetas.update((tags) => tags.filter((t) => t !== tag));
  }

  confirmarCrearCategoriaGasto(nombre: string): void {
    const nameTrim = nombre.trim();
    if (!nameTrim) return;

    const match = this.categoriasDisponibles.find(
      (c) => c.nombre.toLowerCase() === nameTrim.toLowerCase()
    );
    if (match) {
      this.categoria.set(match.id);
      return;
    }

    this.financieroService.crearCategoria({
      nombre: nameTrim,
      descripcion: 'Categoría personalizada de gastos',
      icono: this.iconoCategoria(nameTrim),
      tipo: 'GASTO'
    }).subscribe({
      next: (cat) => {
        this.stateService.categorias.update((cats) => [...cats, cat]);
        this.categoria.set(cat.id);
      },
      error: (err) => {
        console.error('Error al crear categoría de gasto:', err);
      }
    });
  }

  seleccionarSugerenciaGasto(nombre: string): void {
    this.confirmarCrearCategoriaGasto(nombre);
  }

  private validarFormulario(): Record<string, string> {
    const out: Record<string, string> = {};

    if (!this.categoria().trim()) out['categoria'] = 'Selecciona una categoría.';
    if (!this.monto().trim() || Number(this.monto()) <= 0) out['monto'] = 'Ingresa un monto válido mayor a 0.';
    if (!this.nombreGasto().trim()) out['nombreGasto'] = 'Ingresa el nombre del gasto.';
    if (!this.descripcion().trim()) out['descripcion'] = 'Ingresa una descripción del gasto.';
    if (!this.fecha().trim()) out['fecha'] = 'Selecciona una fecha.';

    return out;
  }

  private cargarPendientesLocales(): void {
    const storage = globalThis.localStorage;
    if (!storage) return;
    try {
      const raw = storage.getItem(this.pendientesStorageKey);
      if (!raw) return;
      const pendientes = JSON.parse(raw);
      if (Array.isArray(pendientes)) this.pendientesMock.set(pendientes);
    } catch {
      storage.removeItem(this.pendientesStorageKey);
    }
  }

  private guardarPendientesLocales(pendientes: Array<{
    id: string;
    nombre: string;
    frecuencia: 'MENSUAL' | 'SEMANAL' | 'QUINCENAL';
    fechaVencimiento: string;
    monto: number;
    vencePronto: boolean;
    metodoPago: MetodoPago;
    categoriaIcono: string;
  }>): void {
    const storage = globalThis.localStorage;
    if (!storage) return;
    try {
      storage.setItem(this.pendientesStorageKey, JSON.stringify(pendientes));
    } catch {
      this.mensajeFormulario.set('No se pudo guardar el pendiente localmente.');
    }
  }

  private venceEnDias(fecha: Date, dias: number): boolean {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const limite = new Date(hoy);
    limite.setDate(hoy.getDate() + dias);
    const vencimiento = new Date(fecha);
    vencimiento.setHours(0, 0, 0, 0);
    return vencimiento >= hoy && vencimiento <= limite;
  }

  private iconoCategoria(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('comida')) return 'utensils';
    if (key.includes('hogar')) return 'house';
    if (key.includes('transport')) return 'bus';
    if (key.includes('servicio')) return 'wifi';
    if (key.includes('entreten')) return 'film';
    if (key.includes('salud')) return 'briefcase-medical';
    return 'receipt';
  }
}

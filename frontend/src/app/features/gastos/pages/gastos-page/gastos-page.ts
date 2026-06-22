import { Component, computed, inject, signal, effect, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Transacciones } from '../../../../core/services/transacciones';
import { MetodoPago, TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { AuthService } from '../../../../core/services/auth.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { IaService } from '../../../../core/services/ia.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-gastos-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './gastos-page.html',
  styleUrl: './gastos-page.scss'
})
export class GastosPage implements OnDestroy {
  private readonly transaccionesService = inject(Transacciones);
  private readonly authService = inject(AuthService);
  private readonly financieroService = inject(FinancieroService);
  private readonly eventBus = inject(AppEventBus);
  private readonly stateService = inject(GastosStateService);
  private readonly iaService = inject(IaService);
private readonly router = inject(Router);
  private readonly pendientesStorageKey = 'luka:gastos:pendientes-locales';
  readonly sugerenciasIa = signal<string[]>([]);
  readonly clasificandoIa = signal(false);
  readonly intentosIaRestantes = computed(() => this.iaService.clasificacionesRestantes());
  readonly intentosIaMaximos = computed(() => this.iaService.clasificacionesMaximas());
  readonly puedeSugerirCategoriaIa = computed(() =>
    this.descripcion().trim().length >= 4 && !this.clasificandoIa() && this.intentosIaRestantes() > 0
  );
  readonly cargando = computed(() => this.stateService.cargando());
  readonly terminoBusqueda = signal('');
  readonly tabActiva = signal<'todos' | 'pagados' | 'pendientes' | 'recurrentes'>('todos');
  readonly gastos = computed(() => this.stateService.gastos());
  readonly modalAbierto = signal(false);
  readonly guardandoGasto = signal(false);
  readonly mensajeFormulario = signal('');
  readonly gastoEditandoId = signal<string | null>(null);
  readonly gastoPendienteEliminar = signal<{ id: string; nombre: string } | null>(null);
readonly cantidadPendientes = computed(
  () => this.gastosPendientes().length
);

readonly pendientePorPagar = computed(() => ({
  total: this.totalPendiente(),
  cantidad: this.cantidadPendientes(),
  variacion: this.variacionPendienteMensual()
}));
  readonly categoria = signal('');
  readonly monto = signal('');
  readonly nombreGasto = signal('');
  readonly descripcion = signal('');
  readonly fecha = signal('');
  readonly metodoPago = signal<MetodoPago>('DIGITAL');
  readonly registrarComoPendiente = signal(false);
  readonly etiquetas = signal<string[]>([]);
  readonly nuevaEtiqueta = signal('');
  readonly filtroTendencia = signal<'7d' | '30d' | '90d'>('30d');
  readonly mesSeleccionado = signal<string>('Todos');
  readonly fechaSeleccionada = signal<string>('');
  readonly errores = signal<Record<string, string>>({});
  readonly eliminadosIds = signal<string[]>([]);

  readonly saldoActual = computed(() => Number(this.stateService.resumenActual()?.balance ?? 0));
  readonly totalGastadoActual = computed(() => Number(this.stateService.resumenActual()?.totalGastos ?? 0));
  readonly totalGastosAnterior = computed(() => Number(this.stateService.resumenAnterior()?.totalGastos ?? 0));
  readonly saldoAnterior = computed(() => Number(this.stateService.resumenAnterior()?.balance ?? 0));

  readonly variacionGastado = computed(() => this.calcularVariacion(this.totalGastadoActual(), this.totalGastosAnterior()));
  readonly variacionSaldo = computed(() => this.calcularVariacion(this.saldoActual(), this.saldoAnterior()));
  readonly variacionPendiente = signal(0);
  readonly bannerIntegracion = signal(
    'Integración en curso: historial de gastos (OK). Pendientes/Recurrentes dependen de Suscripciones (falta implementar backend).'
  );

  readonly pendientesMock = signal<Array<{
    id: string;
    nombre: string;
    frecuencia: 'MENSUAL' | 'SEMANAL' | 'QUINCENAL';
    fechaVencimiento: string;
    monto: number;
    vencePronto: boolean;
    metodoPago: MetodoPago;
    categoriaIcono: string;
  }>>([
    {
      id: 'pend-1',
      nombre: 'Internet',
      frecuencia: 'MENSUAL',
      fechaVencimiento: '15/07/2025',
      monto: 79.9,
      vencePronto: true,
      metodoPago: 'DIGITAL',
      categoriaIcono: 'wifi'
    },
    {
      id: 'pend-2',
      nombre: 'Streaming',
      frecuencia: 'MENSUAL',
      fechaVencimiento: '20/07/2025',
      monto: 24,
      vencePronto: false,
      metodoPago: 'TARJETA',
      categoriaIcono: 'film'
    }
  ]);

  readonly pagadosMock = signal<Array<{
    id: string;
    nombre: string;
    detalle: string;
    categoria: string;
    fecha: string;
    hora: string;
    monto: number;
    metodo: string;
    estado: 'Pagado' | 'Pendiente';
    icono: string;
    colorCategoria: 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud';
  }>>([
    {
      id: 'mock-1',
      nombre: 'Almuerzo',
      detalle: 'Comida corporativa',
      categoria: 'Alimentos',
      fecha: 'Hoy',
      hora: '13:00',
      monto: 24.5,
      metodo: 'DIGITAL',
      estado: 'Pagado',
      icono: 'utensils',
      colorCategoria: 'comida'
    },
    {
      id: 'mock-2',
      nombre: 'Gasolina',
      detalle: 'Recarga semanal',
      categoria: 'Transporte',
      fecha: 'Ayer',
      hora: '08:30',
      monto: 18,
      metodo: 'TARJETA',
      estado: 'Pagado',
      icono: 'bus',
      colorCategoria: 'transporte'
    }
  ]);

  readonly usarMockVisualPagados = signal(true);

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

  readonly totalGastado = computed(() =>
    this.filasPagadas().reduce((acc, gasto) => acc + Number(gasto.monto || 0), 0)
  );

  readonly totalPendiente = computed(() =>
    this.gastosPendientes().reduce((acc, p) => acc + Number(p.monto || 0), 0)
  );

  readonly totalPagado = computed(() =>
    this.filasPagadas().filter((g) => g.estado === 'Pagado').reduce((acc, g) => acc + g.monto, 0)
  );

  readonly proximoVencimiento = computed(() => this.gastosPendientes().find(() => true) ?? null);

  readonly gastosPorCategoria = computed(() => {
    const grupos = new Map<string, { categoria: string; total: number }>();
    for (const g of this.filasPagadas()) {
      const key = g.categoria || 'Otros';
      const prev = grupos.get(key);
      grupos.set(key, { categoria: key, total: (prev?.total ?? 0) + Number(g.monto || 0) });
    }

    const total = Array.from(grupos.values()).reduce((acc, item) => acc + item.total, 0);
    return Array.from(grupos.values())
      .sort((a, b) => b.total - a.total)
      .map((item) => ({
        ...item,
        porcentaje: total > 0 ? (item.total / total) * 100 : 0,
      }));
  });

  readonly tendenciaMensual = computed(() => {
    const meses = new Map<string, { etiqueta: string; total: number; fecha: Date }>();
    for (const g of this.filasPagadas()) {
      const fecha = this.parseFechaFila(g.fecha);
      const key = this.monthKey(fecha);
      const etiqueta = fecha.toLocaleDateString('es-PE', { month: 'short' });
      const prev = meses.get(key);
      meses.set(key, {
        etiqueta,
        fecha: new Date(fecha.getFullYear(), fecha.getMonth(), 1),
        total: (prev?.total ?? 0) + Number(g.monto || 0)
      });
    }

    const arr = Array.from(meses.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([, v]) => v);

    const max = Math.max(...arr.map((x) => x.total), 1);
    return arr.map((x) => ({ ...x, porcentaje: (x.total / max) * 100 }));
  });

  readonly tendenciaGastosMensuales = computed(() => {
    const totales = new Map<string, number>();
    for (const g of this.filasPagadas()) {
      const fecha = this.parseFechaFila(g.fecha);
      const key = this.monthKey(fecha);
      totales.set(key, (totales.get(key) ?? 0) + Number(g.monto || 0));
    }

    const hoy = new Date();
    const salida: Array<{ fecha: Date; etiqueta: string; total: number; porcentaje: number }> = [];
    for (let i = 5; i >= 0; i--) {
      const fecha = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      salida.push({
        fecha,
        etiqueta: fecha.toLocaleDateString('es-PE', { month: 'short' }),
        total: totales.get(this.monthKey(fecha)) ?? 0,
        porcentaje: 0
      });
    }

    const max = Math.max(...salida.map((item) => item.total), 1);
    return salida.map((item) => ({
      ...item,
      porcentaje: (item.total / max) * 100
    }));
  });

  readonly kpiGastosLinePath = computed(() => this.buildSparklinePath(this.tendenciaGastosMensuales()));

  readonly kpiGastosLineFillPath = computed(() => {
    const line = this.kpiGastosLinePath();
    return line ? `${line} L114 62 L6 62 Z` : '';
  });

  readonly tendenciaPendientesMensuales = computed(() => {
    const totales = new Map<string, number>();
    for (const pendiente of this.gastosPendientes()) {
      const fecha = this.parseFechaFila(pendiente.fechaVencimiento);
      const key = this.monthKey(fecha);
      totales.set(key, (totales.get(key) ?? 0) + Number(pendiente.monto || 0));
    }

    const hoy = new Date();
    const salida: Array<{ fecha: Date; total: number; porcentaje: number }> = [];
    for (let i = 5; i >= 0; i--) {
      const fecha = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      salida.push({
        fecha,
        total: totales.get(this.monthKey(fecha)) ?? 0,
        porcentaje: 0
      });
    }

    const max = Math.max(...salida.map((item) => item.total), 1);
    return salida.map((item) => ({
      ...item,
      porcentaje: (item.total / max) * 100
    }));
  });

  readonly kpiPendientesBars = computed(() =>
    this.tendenciaPendientesMensuales().map((item) =>
      item.total > 0 ? Math.max(12, Math.round(item.porcentaje)) : 0
    )
  );

  readonly variacionPendienteMensual = computed(() => {
    const actual = new Date();
    const previo = new Date(actual.getFullYear(), actual.getMonth() - 1, 1);
    const data = this.tendenciaPendientesMensuales();
    const totalActual = data.find((item) => this.monthKey(item.fecha) === this.monthKey(actual))?.total ?? 0;
    const totalPrevio = data.find((item) => this.monthKey(item.fecha) === this.monthKey(previo))?.total ?? 0;
    return this.calcularVariacionMensual(totalActual, totalPrevio);
  });

  readonly donutCategorias = computed(() => {
    const colores = ['#f59e0b', '#10b981', '#6366f1', '#ec4899', '#94a3b8', '#14b8a6'];
    let offset = 100;
    return this.gastosPorCategoria().map((item, idx) => {
      const porcentaje = Math.max(0, Math.min(100, Number(item.porcentaje || 0)));
      const segmento = {
        ...item,
        color: colores[idx % colores.length],
        dasharray: `${porcentaje} ${Math.max(0, 100 - porcentaje)}`,
        dashoffset: offset,
      };
      offset -= porcentaje;
      return segmento;
    });
  });

  readonly totalDonutCategorias = computed(() =>
    this.gastosPorCategoria().reduce((acc, item) => acc + Number(item.total || 0), 0)
  );

  readonly tendenciaMensualFiltrada = computed(() => {
    const dias = this.filtroTendencia() === '7d' ? 7 : this.filtroTendencia() === '30d' ? 30 : 90;
    const hoy = new Date();
    const desde = new Date(hoy);
    desde.setDate(hoy.getDate() - dias);

    const totalesDia = new Map<string, number>();
    for (const g of this.filasPagadas()) {
      const fecha = this.parseFechaFila(g.fecha);
      if (fecha < desde || fecha > hoy) continue;
      const key = `${fecha.getFullYear()}-${fecha.getMonth()}-${fecha.getDate()}`;
      totalesDia.set(key, (totalesDia.get(key) ?? 0) + Number(g.monto || 0));
    }

    const step = dias === 90 ? 7 : 1;
    const salida: Array<{ fecha: Date; etiqueta: string; total: number }> = [];
    const cursor = new Date(desde);
    while (cursor <= hoy) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`;
      salida.push({
        fecha: new Date(cursor),
        etiqueta: cursor.toLocaleDateString('es-PE', { day: '2-digit', month: 'short' }),
        total: totalesDia.get(key) ?? 0,
      });
      cursor.setDate(cursor.getDate() + step);
    }
    return salida;
  });

  readonly topDiasGasto = computed(() => {
    const data = this.tendenciaMensualFiltrada()
      .filter((d) => d.total > 0)
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);

    const max = Math.max(...data.map((d) => d.total), 1);
    return data.map((d) => ({
      ...d,
      porcentaje: (d.total / max) * 100,
    }));
  });

  readonly gastoPromedioMensual = computed(() => {
    const actual = new Date();
    const keyActual = this.monthKey(actual);
    return this.tendenciaGastosMensuales().find((item) => this.monthKey(item.fecha) === keyActual)?.total ?? 0;
  });

  readonly variacionPromedioMensual = computed(() => {
    const actual = new Date();
    const previo = new Date(actual.getFullYear(), actual.getMonth() - 1, 1);
    const data = this.tendenciaGastosMensuales();
    const totalActual = data.find((item) => this.monthKey(item.fecha) === this.monthKey(actual))?.total ?? 0;
    const totalPrevio = data.find((item) => this.monthKey(item.fecha) === this.monthKey(previo))?.total ?? 0;
    return this.calcularVariacion(totalActual, totalPrevio);
  });

  readonly gastosPendientes = computed(() => this.pendientesMock());
  readonly gastosPagados = computed(() => this.gastos());

  readonly filasPagadas = computed(() => {
    const eliminados = new Set(this.eliminadosIds());
    const base = this.usarMockVisualPagados()
      ? this.pagadosMock()
      : this.gastosPagados().map((g) => {
          const fecha = new Date(g.fechaTransaccion);
          const categoria = g.categoria || 'Otros';
          const { nombre, detalle } = this.parseNotas(g.notas, categoria);
          return {
            id: g.id,
            nombre,
            detalle,
            categoria,
            categoriaId: g.categoriaId,
            fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
            hora: fecha.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' }),
            monto: Number(g.monto || 0),
            metodo: g.metodoPago || 'DIGITAL',
            estado: 'Pagado' as const,
            icono: g.categoriaIcono || this.iconoCategoria(categoria),
            colorCategoria: this.colorCategoria(categoria),
          };
        });

    return base.filter((g) => !eliminados.has(g.id));
  });

  readonly meses = computed(() => {
    const mesesSet = new Set<string>();
    for (const gasto of this.filasPagadas()) {
      const fecha = this.parseFechaFila(gasto.fecha);
      const mes = fecha.toLocaleDateString('es-PE', {
        year: 'numeric',
        month: 'long'
      });
      mesesSet.add(mes);
    }
    return Array.from(mesesSet);
  });

 readonly gastosFiltradosPorFecha = computed(() => {
  // Si no hay fecha seleccionada → mostrar todo
  if (!this.fechaSeleccionada()) {
    return this.filasPagadas();
  }

  const seleccionada = new Date(this.fechaSeleccionada());

  return this.filasPagadas().filter((g) => {
    const fecha = this.parseFechaFila(g.fecha);

    return (
      fecha.getFullYear() === seleccionada.getFullYear() &&
      fecha.getMonth() === seleccionada.getMonth() &&
      fecha.getDate() === seleccionada.getDate()
    );
  });
});
  readonly categoriasUI = computed(() => {
    return this.gastosPorCategoria().map((cat) => ({
      name: cat.categoria,
      percent: Math.round(cat.porcentaje),
      amount: `S/ ${cat.total.toFixed(2)}`,
      color: this.getColor(cat.categoria),
      icon: this.getIcon(cat.categoria)
    }));
  });

  readonly categoriasUIFiltradas = computed(() => {
    return this.categoriasUI().map((cat) => ({
      ...cat,
      percent: Math.max(0, Math.min(cat.percent, 100))
    }));
  });

  readonly gastosFiltradosPagados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    const tab = this.tabActiva();
    return this.filasPagadas().filter((gasto) => {
      const coincideBusqueda = !q ||
        gasto.nombre.toLowerCase().includes(q) ||
        gasto.categoria.toLowerCase().includes(q) ||
        gasto.metodo.toLowerCase().includes(q);
      const coincideTab = tab === 'todos' || tab === 'pagados';
      return coincideBusqueda && coincideTab;
    });
  });

  readonly pendientesFiltrados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    const tab = this.tabActiva();
    return this.gastosPendientes().filter((p) => {
      const coincideBusqueda =
        !q || p.nombre.toLowerCase().includes(q) || p.frecuencia.toLowerCase().includes(q);
      const coincideTab = tab === 'todos' || tab === 'pendientes' || tab === 'recurrentes';
      if (tab === 'recurrentes') {
        return coincideBusqueda && p.frecuencia !== 'SEMANAL';
      }
      return coincideBusqueda && coincideTab;
    });
  });

  private txSub?: { unsubscribe: () => void };

  constructor() {
    this.cargarPendientesLocales();
    this.stateService.cargarDatos();

    effect(() => {
      if (this.stateService.gastos().length > 0) {
        this.usarMockVisualPagados.set(false);
      } else {
        this.usarMockVisualPagados.set(true);
      }
    });

    effect(() => {
      this.guardarPendientesLocales(this.pendientesMock());
    });

    this.txSub = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      this.stateService.invalidarCache();
    });
  }

  ngOnDestroy(): void {
   this.txSub?.unsubscribe();
 

  }

  seleccionarTab(tab: 'todos' | 'pagados' | 'pendientes' | 'recurrentes'): void {
    this.tabActiva.set(tab);
  }

  actualizarBusqueda(valor: string): void {
    this.terminoBusqueda.set(valor);
  }

  marcarPendienteComoPagado(id: string): void {
    const pendiente = this.pendientesMock().find((p) => p.id === id);
    if (!pendiente) {
      return;
    }

    this.pendientesMock.set(this.pendientesMock().filter((p) => p.id !== id));
    this.pagadosMock.update((items) => [
      {
        id: `mock-${pendiente.id}`,
        nombre: pendiente.nombre,
        detalle: 'Suscripción recurrente',
        categoria: 'Servicios',
        fecha: 'Hoy',
        hora: 'Ahora',
        monto: pendiente.monto,
        metodo: pendiente.metodoPago,
        estado: 'Pagado',
        icono: 'circle-check',
        colorCategoria: 'servicios',
      },
      ...items,
    ]);
    this.usarMockVisualPagados.set(true);
  }

  abrirModal(): void {
    this.resetFormulario();
    this.modalAbierto.set(true);
  }

  editarGasto(id: string): void {
    const gasto = this.filasPagadas().find((g) => g.id === id);
    if (!gasto) return;

    this.gastoEditandoId.set(id);
    this.nombreGasto.set(gasto.nombre);
    this.descripcion.set(gasto.detalle);
    this.monto.set(String(gasto.monto));
    this.fecha.set(this.fechaIsoDesdeTexto(gasto.fecha));
    this.metodoPago.set(this.normalizarMetodoPago(gasto.metodo));

    let catId = (gasto as any).categoriaId || '';
    if (!catId) {
      const match = this.categoriasDisponibles.find(
        (c) => c.nombre.toLowerCase() === gasto.categoria.toLowerCase()
      );
      catId = match ? match.id : '';
    }
    this.categoria.set(catId);
    this.modalAbierto.set(true);
  }

  eliminarGasto(id: string): void {
    const gasto = this.filasPagadas().find((g) => g.id === id);
    this.gastoPendienteEliminar.set({ id, nombre: gasto?.nombre ?? 'este gasto' });
  }

  confirmarEliminarGasto(): void {
    const pendiente = this.gastoPendienteEliminar();
    if (!pendiente) return;
    const id = pendiente.id;

    if (id.startsWith('g') || id.startsWith('mock-')) {
      this.eliminadosIds.update((ids) => Array.from(new Set([...ids, id])));
      this.pagadosMock.update((items) => items.filter((i) => i.id !== id));
      this.usarMockVisualPagados.set(true);
      this.gastoPendienteEliminar.set(null);
      return;
    }

    this.transaccionesService.eliminar(id).subscribe({
      next: () => {
        this.gastoPendienteEliminar.set(null);
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
      },
      error: () => this.mensajeFormulario.set('No se pudo eliminar el gasto.')
    });
  }

  cancelarEliminarGasto(): void {
    this.gastoPendienteEliminar.set(null);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardarGasto(): void {
    const errores = this.validarFormulario();
    this.errores.set(errores);
    this.mensajeFormulario.set('');

    if (Object.keys(errores).length > 0) {
      return;
    }

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

    const editId = this.gastoEditandoId();
    if (editId) {
      if (editId.startsWith('g') || editId.startsWith('mock-')) {
        this.pagadosMock.update((items) =>
          items.map((i) =>
            i.id !== editId
              ? i
              : {
                  ...i,
                  nombre: this.nombreGasto().trim(),
                  detalle: this.descripcion().trim(),
                  monto: Number(this.monto()),
                  metodo: this.metodoPago(),
                  fecha: this.fecha()
                    ? new Date(this.fecha()).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
                    : i.fecha,
                }
          )
        );
        this.usarMockVisualPagados.set(true);
        this.modalAbierto.set(false);
        this.resetFormulario();
        this.guardandoGasto.set(false);
        return;
      }

      const usuarioIdEdit = this.authService.usuario()?.id;
      if (!usuarioIdEdit) {
        this.mensajeFormulario.set('No se encontró sesión activa.');
        this.guardandoGasto.set(false);
        return;
      }

      const requestEdit: TransaccionRequestDTO = {
        usuarioId: usuarioIdEdit,
        nombreCliente: this.authService.usuario()?.nombreUsuario ?? 'Cliente',
        monto: Number(this.monto()),
        tipo: 'GASTO',
        categoriaId: this.categoria() || 'otros',
        fechaTransaccion: getLocalIsoString(this.fecha()),
        metodoPago: this.metodoPago(),
        notas: `${this.nombreGasto().trim()}|${this.descripcion().trim()}`,
        descripcion: this.descripcion().trim(),
        etiquetas: this.etiquetas().join(','),
      };

      this.transaccionesService.actualizar(editId, requestEdit).subscribe({
        next: () => {
          this.guardandoGasto.set(false);
          this.modalAbierto.set(false);
          this.resetFormulario();
          this.stateService.invalidarCache();
          this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        },
        error: () => {
          this.guardandoGasto.set(false);
          this.mensajeFormulario.set('No se pudo actualizar el gasto.');
        },
      });
      return;
    }

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
      this.guardandoGasto.set(false);
      this.modalAbierto.set(false);
      this.resetFormulario();
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
      etiquetas: this.etiquetas().join(','),
    };

    this.transaccionesService.registrar(request).subscribe({
      next: () => {
        this.guardandoGasto.set(false);
        this.modalAbierto.set(false);
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
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
          if (categorias) {
            this.sugerenciasIa.set(categorias);
          }
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

    if (!this.categoria().trim()) {
      out['categoria'] = 'Selecciona una categoría.';
    }
    if (!this.monto().trim() || Number(this.monto()) <= 0) {
      out['monto'] = 'Ingresa un monto válido mayor a 0.';
    }
    if (!this.nombreGasto().trim()) {
      out['nombreGasto'] = 'Ingresa el nombre del gasto.';
    }
    if (!this.descripcion().trim()) {
      out['descripcion'] = 'Ingresa una descripción del gasto.';
    }
    if (!this.fecha().trim()) {
      out['fecha'] = 'Selecciona una fecha.';
    }

    return out;
  }

  private resetFormulario(): void {
    this.gastoEditandoId.set(null);
    this.categoria.set('');
    this.monto.set('');
    this.nombreGasto.set('');
    this.descripcion.set('');
    this.fecha.set('');
    this.metodoPago.set('DIGITAL');
    this.registrarComoPendiente.set(false);
    this.etiquetas.set([]);
    this.nuevaEtiqueta.set('');
    this.errores.set({});
    this.mensajeFormulario.set('');
    this.sugerenciasIa.set([]);
  }

  private parseNotas(notas: string | null, fallbackCategoria: string): { nombre: string; detalle: string } {
    if (!notas) {
      return { nombre: fallbackCategoria, detalle: 'Transacción registrada' };
    }

    const [nombreRaw, detalleRaw] = notas.split('|');
    const nombre = nombreRaw?.trim() || fallbackCategoria;
    const detalle = detalleRaw?.trim() || 'Transacción registrada';
    return { nombre, detalle };
  }

  private parseFechaFila(fechaTexto: string): Date {
    if (fechaTexto === 'Hoy') return new Date();
    if (fechaTexto === 'Ayer') {
      const ayer = new Date();
      ayer.setDate(ayer.getDate() - 1);
      return ayer;
    }

    const meses: Record<string, number> = {
      ene: 0,
      enero: 0,
      feb: 1,
      febrero: 1,
      mar: 2,
      marzo: 2,
      abr: 3,
      abril: 3,
      may: 4,
      mayo: 4,
      jun: 5,
      junio: 5,
      jul: 6,
      julio: 6,
      ago: 7,
      agosto: 7,
      sep: 8,
      sept: 8,
      septiembre: 8,
      oct: 9,
      octubre: 9,
      nov: 10,
      noviembre: 10,
      dic: 11,
      diciembre: 11
    };
    const limpia = fechaTexto.toLowerCase().replace(/\./g, '').replace(/,/g, '').trim();
    const partes = limpia.split(/\s+/);
    const dia = Number(partes[0]);
    const mesTexto = partes[1] ?? '';
    const mes = meses[mesTexto];
    const anio = Number(partes[2]) || new Date().getFullYear();
    if (Number.isFinite(dia) && mes !== undefined) {
      return new Date(anio, mes, dia);
    }

    const dt = new Date(fechaTexto);
    if (!Number.isNaN(dt.getTime())) return dt;
    return new Date();
  }

  private cargarPendientesLocales(): void {
    const storage = globalThis.localStorage;
    if (!storage) return;

    try {
      const raw = storage.getItem(this.pendientesStorageKey);
      if (!raw) return;
      const pendientes = JSON.parse(raw);
      if (Array.isArray(pendientes)) {
        this.pendientesMock.set(pendientes);
      }
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

  private monthKey(fecha: Date): string {
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`;
  }

  private buildSparklinePath(data: Array<{ total: number }>): string {
    if (!data.length) return '';

    const width = 108;
    const height = 46;
    const x0 = 6;
    const y0 = 8;
    const max = Math.max(...data.map((item) => item.total), 1);
    const step = data.length > 1 ? width / (data.length - 1) : width;
    const points = data.map((item, index) => {
      const x = x0 + index * step;
      const y = y0 + height - (item.total / max) * height;
      return { x, y };
    });

    if (points.length === 1) {
      const point = points[0]!;
      return `M${point.x} ${point.y} L${x0 + width} ${point.y}`;
    }

    return points.reduce((path, point, index) => {
      if (index === 0) return `M${point.x} ${point.y}`;
      const prev = points[index - 1]!;
      const controlOffset = step * 0.42;
      return `${path} C${prev.x + controlOffset} ${prev.y} ${point.x - controlOffset} ${point.y} ${point.x} ${point.y}`;
    }, '');
  }

  private fechaIsoDesdeTexto(fechaTexto: string): string {
    const dt = this.parseFechaFila(fechaTexto);
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private normalizarMetodoPago(valor: string): MetodoPago {
    const v = valor.toLowerCase();
    if (v.includes('efectivo')) return 'EFECTIVO';
    if (v.includes('transfer')) return 'TRANSFERENCIA';
    if (v.includes('tarjeta') || v.includes('crédito') || v.includes('débito')) return 'TARJETA';
    return 'DIGITAL';
  }

  private cargarGastos(): void {
    this.stateService.cargarDatos();
  }

  private calcularVariacion(actual: number, previo: number): number {
    if (!previo) return 0;
    return ((actual - previo) / Math.abs(previo)) * 100;
  }

  private calcularVariacionMensual(actual: number, previo: number): number {
    if (!previo) return actual > 0 ? 100 : 0;
    return ((actual - previo) / Math.abs(previo)) * 100;
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

  private colorCategoria(categoria: string): 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud' {
    const key = categoria.toLowerCase();
    if (key.includes('comida')) return 'comida';
    if (key.includes('hogar')) return 'hogar';
    if (key.includes('transport')) return 'transporte';
    if (key.includes('servicio')) return 'servicios';
    if (key.includes('entreten')) return 'entretenimiento';
    return 'salud';
  }

  private getColor(cat: string): string {
    const key = cat.toLowerCase();
    if (key.includes('comida')) return '#10b981';
    if (key.includes('hogar')) return '#2563eb';
    if (key.includes('transporte')) return '#8b5cf6';
    if (key.includes('entreten')) return '#f97316';
    return '#9ca3af';
  }

  private getIcon(cat: string): string {
    const key = cat.toLowerCase();
    if (key.includes('comida')) return '🍔';
    if (key.includes('hogar')) return '🏠';
    if (key.includes('transporte')) return '🚗';
    if (key.includes('entreten')) return '🎮';
    return '⋯';
  }
}

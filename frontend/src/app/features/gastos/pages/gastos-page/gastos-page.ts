import { Component, ElementRef, ViewChild, computed, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Transacciones } from '../../../../core/services/transacciones';
import { MetodoPago, TransaccionDTO, TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { AuthService } from '../../../../core/services/auth.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { forkJoin } from 'rxjs';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { IaService } from '../../../../core/services/ia.service';

type TourStep = {
  id: string;
  targetId: string;
  titulo: string;
  descripcion: string[];
};

type TourRect = {
  top: number;
  left: number;
  width: number;
  height: number;
};

type TourCardRect = {
  top: number;
  left: number;
  width: number;
};

@Component({
  selector: 'app-gastos-page',
  standalone:true,
  imports: [CommonModule],
  templateUrl: './gastos-page.html',
  styleUrl: './gastos-page.scss',
})
export class GastosPage {
  @ViewChild('fechaInput') private fechaInput?: ElementRef<HTMLInputElement>;

  private readonly transaccionesService = inject(Transacciones);
  private readonly authService = inject(AuthService);
  private readonly financieroService = inject(FinancieroService);
  private readonly eventBus = inject(AppEventBus);
  private readonly stateService = inject(GastosStateService);
  private readonly iaService = inject(IaService);

  readonly sugerenciasIa = signal<string[]>([]);
  readonly iaMensaje = signal('');
  readonly clasificandoIa = signal(false);
  readonly cargando = computed(() => this.stateService.cargando());
  readonly terminoBusqueda = signal('');
  readonly tabActiva = signal<'todos' | 'pagados' | 'pendientes' | 'recurrentes'>('todos');
  readonly gastos = computed(() => this.stateService.gastos());
  readonly modalAbierto = signal(false);
  readonly guardandoGasto = signal(false);
  readonly mensajeFormulario = signal('');
  readonly gastoEditandoId = signal<string | null>(null);
  readonly gastoPendienteEliminar = signal<{ id: string; nombre: string } | null>(null);
  readonly pendientesPagadosIds = signal<string[]>([]);
  readonly filtrosAbiertos = signal(false);
  readonly filtroNombre = signal('');
  readonly filtroCategoriaId = signal('');
  readonly filtroFechaDesde = signal('');
  readonly filtroFechaHasta = signal('');
  readonly filtroMontoMin = signal('');
  readonly filtroMontoMax = signal('');
  readonly seleccionExportacionIds = signal<string[]>([]);

  readonly categoria = signal('');
  readonly monto = signal('');
  readonly nombreGasto = signal('');
  readonly descripcion = signal('');
  readonly fecha = signal('');
  readonly metodoPago = signal<MetodoPago>('DIGITAL');
  readonly tipoFrecuencia = signal<'DIARIO' | 'RECURRENTE'>('DIARIO');
  readonly etiquetas = signal<string[]>([]);
  readonly nuevaEtiqueta = signal('');
  readonly filtroTendencia = signal<'7d' | '30d' | '90d'>('30d');
  readonly errores = signal<Record<string, string>>({});
  readonly eliminadosIds = signal<string[]>([]);
  readonly descripcionPalabras = computed(() => this.contarPalabras(this.descripcion()));
  readonly formularioCompleto = computed(() =>
    this.categoria().trim() !== '' &&
    this.nombreGasto().trim() !== '' &&
    this.descripcion().trim() !== '' &&
    this.contarPalabras(this.descripcion()) <= 200 &&
    this.fecha().trim() !== '' &&
    Number(this.monto()) > 0
  );
  readonly tourAbierto = signal(false);
  readonly tourPasoIndex = signal(0);
  readonly tourRect = signal<TourRect | null>(null);
  readonly tourCardRect = signal<TourCardRect | null>(null);
  readonly tourPasos: TourStep[] = [
    {
      id: 'paso-1',
      targetId: 'tour-bienvenida',
      titulo: 'Bienvenido al módulo de Gestión de Gastos',
      descripcion: [
        'Aquí podrás registrar tus gastos, controlar pagos pendientes y visualizar reportes financieros en tiempo real.'
      ],
    },
    {
      id: 'paso-2',
      targetId: 'tour-registrar',
      titulo: 'Registrar un nuevo gasto',
      descripcion: [
        'Haz clic aquí para registrar un egreso.',
        'Podrás ingresar monto, categoría, descripción, fecha y método de pago.',
        'Cada gasto se actualizará automáticamente en indicadores y gráficos.'
      ],
    },
    {
      id: 'paso-3',
      targetId: 'tour-kpis',
      titulo: 'Tarjetas KPI',
      descripcion: [
        'Total gastado: monto del período, variación vs mes anterior y tendencia.',
        'Pendiente por pagar: total y cantidad de pagos no realizados.',
        'Próximo vencimiento: nombre, monto, fecha y días restantes.'
      ],
    },
    {
      id: 'paso-4',
      targetId: 'tour-buscar-exportar',
      titulo: 'Buscar y exportar información',
      descripcion: [
        'Busca gastos por palabra clave.',
        'Exporta tus datos en Excel o PDF para reportes.'
      ],
    },
    {
      id: 'paso-5',
      targetId: 'tour-pendientes',
      titulo: 'Pendientes y recurrentes',
      descripcion: [
        'Aquí ves pagos programados con icono, nombre, categoría, frecuencia, fecha, método, estado, tiempo restante y monto.'
      ],
    },
    {
      id: 'paso-6',
      targetId: 'tour-resumen-mes',
      titulo: 'Resumen del mes',
      descripcion: [
        'Panel con Gastado, Saldo y Pendiente.',
        'Incluye promedio diario, mayor categoría, próximo pago y últimos gastos.'
      ],
    },
    {
      id: 'paso-7',
      targetId: 'tour-categorias-anillo',
      titulo: 'Gastos por categoría',
      descripcion: [
        'El gráfico de anillo muestra distribución de gasto, porcentaje por categoría y total del período.'
      ],
    },
    {
      id: 'paso-8',
      targetId: 'tour-periodo-lista',
      titulo: 'Gastos por período',
      descripcion: [
        'Lista reciente con icono, nombre, categoría, fecha y monto.',
        'Puedes cambiar entre 7, 30 o 90 días.'
      ],
    },
    {
      id: 'paso-9',
      targetId: 'tour-pantalla-completa',
      titulo: '¡Todo listo!',
      descripcion: [
        'Ahora puedes registrar gastos, controlar pendientes, analizar categorías, revisar estadísticas y exportar reportes.'
      ],
    },
  ];
  readonly tourPasoActual = computed(() => this.tourPasos[this.tourPasoIndex()] ?? null);
  readonly tourEsUltimoPaso = computed(() => this.tourPasoIndex() >= this.tourPasos.length - 1);
  private readonly tourStorageKey = 'gastos-tour-v1-completado';
  private tourSyncTimer: ReturnType<typeof setTimeout> | null = null;

  readonly saldoActual = computed(() => Number(this.stateService.resumenActual()?.balance ?? 0));
  readonly totalGastadoActual = computed(() => Number(this.stateService.resumenActual()?.totalGastos ?? 0));
  readonly totalGastosAnterior = computed(() => Number(this.stateService.resumenAnterior()?.totalGastos ?? 0));
  readonly saldoAnterior = computed(() => Number(this.stateService.resumenAnterior()?.balance ?? 0));

  readonly variacionGastado = computed(() => this.calcularVariacion(this.totalGastadoActual(), this.totalGastosAnterior()));
  readonly variacionSaldo = computed(() => this.calcularVariacion(this.saldoActual(), this.saldoAnterior()));
  readonly comparativaGastadoAbsoluta = computed(() => this.totalGastadoActual() - this.totalGastosAnterior());
  readonly comparativaGastadoAbsMonto = computed(() => Math.abs(this.comparativaGastadoAbsoluta()));
  readonly variacionGastadoAbs = computed(() => Math.abs(this.variacionGastado()));
  readonly gastoSubioVsMesAnterior = computed(() => this.variacionGastado() >= 0);
  readonly variacionSaldoAbs = computed(() => Math.abs(this.variacionSaldo()));
  readonly saldoSubioVsMesAnterior = computed(() => this.variacionSaldo() >= 0);
  readonly etiquetaMesActual = computed(() => {
    const etiqueta = new Intl.DateTimeFormat('es-PE', { month: 'long', year: 'numeric' }).format(new Date());
    return etiqueta.charAt(0).toUpperCase() + etiqueta.slice(1);
  });
  readonly totalPendienteAnterior = computed(() => this.pendientesDelMes(this.mesAnterior()).reduce((acc, p) => acc + Number(p.monto || 0), 0));
  readonly variacionPendiente = computed(() => this.calcularVariacion(this.totalPendiente(), this.totalPendienteAnterior()));
  readonly variacionPendienteAbs = computed(() => Math.abs(this.variacionPendiente()));
  readonly pendienteSubioVsMesAnterior = computed(() => this.variacionPendiente() >= 0);
  readonly calendarioVencimientosAbierto = signal(false);
  readonly mesCalendario = signal(new Date());
  readonly fechaCalendarioSeleccionada = signal<string | null>(null);
  readonly gastosPeriodoFiltrados = computed(() => {
    const dias = this.filtroTendencia() === '7d' ? 7 : this.filtroTendencia() === '30d' ? 30 : 90;
    const hoy = new Date();
    const desde = new Date(hoy);
    desde.setHours(0, 0, 0, 0);
    desde.setDate(hoy.getDate() - (dias - 1));

    return [...this.gastosFiltradosToolbar()]
      .map((g) => {
        const fechaOrden = this.parseFechaTransaccion(g.fechaRegistro ?? g.fechaTransaccion);
        const categoria = this.obtenerNombreCategoria(g) || 'Otros';
        const { nombre } = this.parseNotas(g.notas, categoria);
        return {
          id: g.id,
          nombre,
          categoria,
          fechaOrden,
          fecha: fechaOrden.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
          monto: Number(g.monto || 0),
          icono: g.categoriaIcono || this.iconoCategoria(categoria),
        };
      })
      .filter((g) => g.fechaOrden >= desde && g.fechaOrden <= hoy)
      .sort((a, b) => b.fechaOrden.getTime() - a.fechaOrden.getTime());
  });

  readonly totalGastosPeriodo = computed(() =>
    this.gastosPeriodoFiltrados().reduce((acc, gasto) => acc + Number(gasto.monto || 0), 0)
  );

  readonly tendenciaPendientesFiltrada = computed(() => {
    const dias = this.filtroTendencia() === '7d' ? 7 : this.filtroTendencia() === '30d' ? 30 : 90;
    const hoy = new Date();
    const desde = new Date(hoy);
    desde.setHours(0, 0, 0, 0);
    desde.setDate(hoy.getDate() - (dias - 1));

    const totalesDia = new Map<string, number>();
    for (const gasto of this.gastosPeriodoFiltrados()) {
      const fecha = gasto.fechaOrden;
      if (fecha < desde || fecha > hoy) continue;
      const key = `${fecha.getFullYear()}-${fecha.getMonth()}-${fecha.getDate()}`;
      totalesDia.set(key, (totalesDia.get(key) ?? 0) + Number(gasto.monto || 0));
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

  readonly puntosTendenciaPendientes = computed(() => {
    const data = this.tendenciaPendientesFiltrada();
    if (!data.length) return [] as Array<{ x: number; y: number; etiqueta: string }>;
    const max = Math.max(...data.map((d) => d.total), 1);
    const n = data.length;
    return data.map((d, idx) => {
      const x = n === 1 ? 50 : 10 + (idx * 80) / (n - 1);
      const y = 82 - ((d.total / max) * 54);
      return { x, y, etiqueta: d.etiqueta };
    });
  });

  readonly topDiasPendientes = computed(() => {
    const data = this.tendenciaPendientesFiltrada()
      .filter((d) => d.total > 0)
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);

    const max = Math.max(...data.map((d) => d.total), 1);
    return data.map((d) => ({
      ...d,
      porcentaje: (d.total / max) * 100,
    }));
  });
  readonly diasSemanaCalendario = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  readonly calendarioMesEtiqueta = computed(() =>
    new Intl.DateTimeFormat('es-PE', { month: 'long', year: 'numeric' }).format(this.mesCalendario())
  );
  readonly calendarioDias = computed(() => this.construirCalendarioMes(this.mesCalendario()));

  get categoriasDisponibles(): any[] {
    return this.stateService.categorias().length > 0
      ? this.stateService.categorias()
      : [
          { id: 'alimentos', nombre: 'Alimentos' },
          { id: 'transporte', nombre: 'Transporte' },
          { id: 'servicios', nombre: 'Servicios' },
          { id: 'hogar', nombre: 'Hogar' },
          { id: 'otros', nombre: 'Otros' },
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
    this.gastos().reduce((acc, gasto) => acc + Number(gasto.monto || 0), 0)
  );

  readonly totalPendiente = computed(() =>
    this.gastosPendientes().reduce((acc, p) => acc + Number(p.monto || 0), 0)
  );
  readonly totalPagado = computed(() =>
    this.filasPagadas().filter((g) => g.estado === 'Pagado').reduce((acc, g) => acc + g.monto, 0)
  );
  readonly presupuestoMensual = computed(() => {
    const gastado = this.totalGastadoActual();
    const saldo = this.saldoActual();
    const base = gastado + Math.max(saldo, 0);
    return base > 0 ? base : Math.max(gastado, 1);
  });
  readonly porcentajePresupuestoConsumido = computed(() =>
    Math.max(0, Math.min(100, (this.totalGastadoActual() / this.presupuestoMensual()) * 100))
  );
  readonly promedioDiarioGasto = computed(() => {
    const hoy = new Date();
    const diasTranscurridos = Math.max(1, hoy.getDate());
    return this.totalGastadoActual() / diasTranscurridos;
  });
  readonly categoriaMayorGasto = computed(() => this.gastosPorCategoria()[0] ?? null);
  readonly resumenProximoPago = computed(() => this.gastosPendientes()[0] ?? null);
  readonly totalCuotasPendientes = computed(() => this.gastosPendientes().length);
  readonly ultimaActualizacionResumen = computed(() => {
    const lista = this.gastos();
    if (!lista.length) return 'Sin movimientos';
    const masReciente = [...lista]
      .map((g) => this.parseFechaTransaccion(g.fechaRegistro ?? g.fechaTransaccion))
      .sort((a, b) => b.getTime() - a.getTime())[0];
    return this.fechaRelativa(masReciente);
  });
  readonly proximoVencimiento = computed(() => {
    const seleccion = this.fechaCalendarioSeleccionada();
    const pendientes = this.gastosPendientes();
    const hoy = new Date();
    const desdeHoy = pendientes.filter((p) => this.parseFechaTransaccion(p.fechaOrden) >= this.inicioDelDia(hoy));
    const masProximo = desdeHoy[0] ?? pendientes[0] ?? null;

    if (seleccion) {
      const fechaSeleccionada = this.parseFechaIsoLocal(seleccion);
      const coincidencias = pendientes.filter((p) => this.esMismaFecha(this.parseFechaTransaccion(p.fechaOrden), fechaSeleccionada));
      return coincidencias[0] ?? masProximo;
    }

    return masProximo;
  });
  readonly fechaProximoVencimiento = computed(() => {
    const proximo = this.proximoVencimiento();
    if (!proximo) return '09 Julio';
    return this.formatearFechaRecordatorio(proximo.fechaVencimiento);
  });
  readonly textoProximoVencimiento = computed(() => {
    const proximo = this.proximoVencimiento();
    if (!proximo) return 'En 3 días';
    const dias = this.calcularDiasParaFecha(proximo.fechaVencimiento);
    if (dias === null) return 'Fecha pendiente';
    if (dias <= 0) return 'Vence hoy';
    if (dias === 1) return 'En 1 día';
    return `En ${dias} días`;
  });
  readonly miniBarrasPendiente = computed(() => {
    const montos = this.gastosPendientes().slice(0, 6).map((p) => Number(p.monto || 0));
    if (!montos.length) return [40, 62, 48, 76, 58, 70];
    const max = Math.max(...montos, 1);
    return montos.map((m) => Math.max(22, Math.round((m / max) * 100)));
  });
  readonly miniTendenciaTotalGastadoPuntos = computed(() => {
    const data = this.tendenciaMensualFiltrada().slice(-8);
    if (!data.length) {
      return '0,24 20,18 40,21 60,14 80,16 100,9';
    }

    const max = Math.max(...data.map((d) => d.total), 1);
    return data
      .map((item, idx) => {
        const x = data.length === 1 ? 50 : (idx * 100) / (data.length - 1);
        const y = 28 - ((item.total / max) * 20);
        return `${x},${y}`;
      })
      .join(' ');
  });

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
    const meses = new Map<string, { etiqueta: string; total: number }>();
    for (const g of this.filasPagadas()) {
      const raw = g.fecha === 'Hoy' ? new Date() : new Date(`${g.fecha} ${new Date().getFullYear()}`);
      const fecha = Number.isNaN(raw.getTime()) ? new Date() : raw;
      const key = `${fecha.getFullYear()}-${fecha.getMonth()}`;
      const etiqueta = fecha.toLocaleDateString('es-PE', { month: 'short' });
      const prev = meses.get(key);
      meses.set(key, { etiqueta, total: (prev?.total ?? 0) + Number(g.monto || 0) });
    }

    const arr = Array.from(meses.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([, v]) => v);

    const max = Math.max(...arr.map((x) => x.total), 1);
    return arr.map((x) => ({ ...x, porcentaje: (x.total / max) * 100 }));
  });

  readonly donutCategorias = computed(() => {
    let offset = 100;
    return this.gastosPorCategoria().map((item, idx) => {
      const porcentaje = Math.max(0, Math.min(100, Number(item.porcentaje || 0)));
      const key = item.categoria.toLowerCase();
      const color =
        key.includes('alimento') || key.includes('comida') ? '#1CC88A' :
        key.includes('transport') ? '#FFB547' :
        key.includes('hogar') ? '#4D8DFF' :
        key.includes('servicio') ? '#6D5DF6' :
        key.includes('entreten') ? '#4F46E5' :
        ['#22C55E', '#6D5DF6', '#FFB547', '#4D8DFF'][idx % 4];
      const segmento = {
        ...item,
        color,
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

  readonly tendenciaLineal = computed(() => {
    const data = this.tendenciaMensualFiltrada();
    if (!data.length) {
      return { puntos: '', etiquetas: [] as string[] };
    }
    const max = Math.max(...data.map((d) => d.total), 1);
    const n = data.length;
    const puntos = data
      .map((item, idx) => {
        const x = n === 1 ? 10 : 10 + (idx * 80) / (n - 1);
        const y = 90 - ((item.total / max) * 80);
        return `${x},${y}`;
      })
      .join(' ');

    return {
      puntos,
      etiquetas: data.map((d) => d.etiqueta),
    };
  });

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

  readonly puntosTendencia = computed(() => {
    const data = this.tendenciaMensualFiltrada();
    if (!data.length) return [] as Array<{ x: number; y: number; etiqueta: string }>;
    const max = Math.max(...data.map((d) => d.total), 1);
    const n = data.length;
    return data.map((d, idx) => {
      const x = n === 1 ? 50 : 10 + (idx * 80) / (n - 1);
      const y = 82 - ((d.total / max) * 54);
      return { x, y, etiqueta: d.etiqueta };
    });
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
  readonly gastosRecientesIngresados = computed(() =>
    [...this.gastos()]
      .sort((a, b) => this.parseFechaTransaccion(b.fechaRegistro ?? b.fechaTransaccion).getTime() - this.parseFechaTransaccion(a.fechaRegistro ?? a.fechaTransaccion).getTime())
      .map((g) => {
        const fecha = this.parseFechaTransaccion(g.fechaRegistro ?? g.fechaTransaccion);
        const categoria = this.obtenerNombreCategoria(g);
        return {
          id: g.id,
          nombre: g.notas?.split('|')[0]?.trim() || this.parseNotas(g.notas, categoria).nombre,
          fechaOrden: fecha,
          fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
          monto: Number(g.monto || 0),
          categoria,
          categoriaIcono: g.categoriaIcono || this.iconoCategoria(categoria),
        };
      })
  );

  readonly gastoPromedioMensual = computed(() => {
    const data = this.tendenciaMensual();
    if (!data.length) return 0;
    const total = data.reduce((acc, item) => acc + Number(item.total || 0), 0);
    return total / data.length;
  });

  readonly variacionPromedioMensual = computed(() => {
    const data = this.tendenciaMensual();
    if (data.length < 2) return 0;
    const actual = data[data.length - 1]?.total ?? 0;
    const previo = data[data.length - 2]?.total ?? 0;
    if (!previo) return 0;
    return ((actual - previo) / previo) * 100;
  });

  readonly gastosPendientes = computed(() => {
    const pagados = new Set(this.pendientesPagadosIds());
    return this.gastosFiltradosToolbar()
      .filter((g) => !pagados.has(g.id) && !this.estaMarcadoComoPagado(g.notas))
      .map((g) => {
        const fecha = new Date(g.fechaTransaccion);
        const categoria = this.obtenerNombreCategoria(g);
        const { nombre, frecuencia } = this.parseNotasMetadata(g.notas, categoria || 'Gasto');
        const frecuenciaNormalizada = this.normalizarFrecuencia(frecuencia);
        const diasRestantes = this.calcularDiasRestantes(fecha);
        const estado = this.calcularEstadoPendiente(diasRestantes);
        const prioridad = this.calcularPrioridad(diasRestantes);
        return {
          id: g.id,
          nombre,
          categoria,
          frecuencia: frecuenciaNormalizada,
          fechaVencimiento: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
          monto: Number(g.monto || 0),
          vencePronto: this.esFechaProxima(fecha),
          metodoPago: this.normalizarMetodoPago(g.metodoPago || 'DIGITAL'),
          categoriaIcono: g.categoriaIcono || this.iconoCategoria(categoria || 'Otros'),
          categoriaEmoji: this.iconoCategoriaEmoji(categoria || 'Otros', nombre),
          formaPago: this.formaPagoVisual(g.metodoPago || 'DIGITAL'),
          estado,
          estadoClase: this.claseEstadoPendiente(estado),
          diasRestantes,
          textoDiasRestantes: this.textoDiasRestantes(diasRestantes),
          diasClase: this.claseDiasRestantes(diasRestantes),
          prioridad,
          prioridadClase: this.clasePrioridad(prioridad),
          progresoAnual: frecuenciaNormalizada === 'ANUAL' ? this.progresoAnual(fecha) : null,
          fechaOrden: fecha.getTime(),
        };
      })
      .sort((a, b) => a.fechaOrden - b.fechaOrden);
  });
  readonly gastosPagadosSector = computed(() => {
    const pagados = new Set(this.pendientesPagadosIds());
    return this.gastos()
      .filter((g) => pagados.has(g.id) || this.estaMarcadoComoPagado(g.notas))
      .map((g) => {
        const fecha = this.parseFechaTransaccion(g.fechaRegistro ?? g.fechaTransaccion);
        const categoria = this.obtenerNombreCategoria(g) || 'Otros';
        const { nombre, frecuencia } = this.parseNotasMetadata(g.notas, categoria);
        return {
          id: g.id,
          nombre,
          categoria,
          categoriaEmoji: this.iconoCategoriaEmoji(categoria, nombre),
          frecuencia: this.normalizarFrecuencia(frecuencia),
          fechaVencimiento: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
          monto: Number(g.monto || 0),
          formaPago: this.formaPagoVisual(g.metodoPago || 'DIGITAL'),
          estado: 'Pagado',
          fechaOrden: fecha.getTime(),
        };
      })
      .sort((a, b) => b.fechaOrden - a.fechaOrden)
      .slice(0, 8);
  });
  readonly resumenPendientes = computed(() => {
    const pendientes = this.gastosPendientes();
    const pendientesTotal = pendientes.length;
    const recurrentes = pendientes.filter((p) => p.frecuencia !== 'DIARIO').length;
    const totalPorPagar = pendientes.reduce((acc, p) => acc + Number(p.monto || 0), 0);
    const vencenSemana = pendientes.filter((p) => p.diasRestantes !== null && p.diasRestantes >= 0 && p.diasRestantes <= 7).length;
    return { pendientesTotal, recurrentes, totalPorPagar, vencenSemana };
  });
  readonly gastosPagados = computed(() => this.gastos());

  readonly filasPagadas = computed(() => {
    const eliminados = new Set(this.eliminadosIds());
    const data = this.gastosPagados();
    if (!data.length) {
      return [];
    }

    const base = data.map((g) => {
      const fecha = new Date(g.fechaTransaccion);
      const categoria = this.obtenerNombreCategoria(g) || g.categoriaId || 'Otros';
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

  readonly gastosFiltradosPagados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    const tab = this.tabActiva();
    return this.filasPagadas().filter((gasto) => {
      const coincideBusqueda = !q || gasto.nombre.toLowerCase().includes(q) || gasto.categoria.toLowerCase().includes(q) || gasto.metodo.toLowerCase().includes(q);

      const coincideTab = tab === 'todos' || tab === 'pagados';
      return coincideBusqueda && coincideTab;
    });
  });
  readonly gastosRecientesResumen = computed(() => {
    return this.gastosRecientesIngresados()
      .slice(0, 3);
  });

  readonly pendientesFiltrados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    return this.gastosPendientes().filter((p) => {
      const coincideBusqueda =
        !q || p.nombre.toLowerCase().includes(q) || p.frecuencia.toLowerCase().includes(q);
      return coincideBusqueda;
    });
  });

    readonly gastosFiltradosToolbar = computed(() => {
      const q = this.terminoBusqueda().trim().toLowerCase();
      const nombreFiltro = this.filtroNombre().trim().toLowerCase();
      const categoriaIdFiltro = this.filtroCategoriaId().trim().toLowerCase();
      const fechaDesde = this.filtroFechaDesde();
      const fechaHasta = this.filtroFechaHasta();
      const montoMin = Number(this.filtroMontoMin() || 0);
      const montoMax = Number(this.filtroMontoMax() || 0);
      const tieneMontoMin = this.filtroMontoMin().trim() !== '';
      const tieneMontoMax = this.filtroMontoMax().trim() !== '';
      const desde = fechaDesde ? new Date(`${fechaDesde}T00:00:00`) : null;
      const hasta = fechaHasta ? new Date(`${fechaHasta}T23:59:59`) : null;

      return this.gastos().filter((g) => {
        const categoriaNombre = this.obtenerNombreCategoria(g) || 'Otros';
        const { nombre } = this.parseNotas(g.notas, categoriaNombre);
        const fecha = this.parseFechaTransaccion(g.fechaRegistro ?? g.fechaTransaccion);
        const monto = Number(g.monto || 0);

        const coincideBusqueda =
          !q ||
          nombre.toLowerCase().includes(q) ||
          categoriaNombre.toLowerCase().includes(q) ||
          monto.toFixed(2).includes(q);
        const coincideNombre = !nombreFiltro || nombre.toLowerCase().includes(nombreFiltro);
        const coincideCategoria =
          !categoriaIdFiltro ||
          (g.categoriaId ?? '').toLowerCase() === categoriaIdFiltro ||
          categoriaNombre.toLowerCase().includes(categoriaIdFiltro);
        const coincideDesde = !desde || fecha >= desde;
        const coincideHasta = !hasta || fecha <= hasta;
        const coincideMontoMin = !tieneMontoMin || monto >= montoMin;
        const coincideMontoMax = !tieneMontoMax || monto <= montoMax;

        return (
          coincideBusqueda &&
          coincideNombre &&
          coincideCategoria &&
          coincideDesde &&
          coincideHasta &&
          coincideMontoMin &&
          coincideMontoMax
        );
      });
    });
    readonly coincidenciasBusqueda = computed(() => {
      const q = this.terminoBusqueda().trim();
      if (!q) return [] as Array<{
        id: string;
        nombre: string;
        categoria: string;
        fecha: string;
        monto: number;
        estado: 'Pagado' | 'Pendiente';
      }>;

      const pagadosMarcados = new Set(this.pendientesPagadosIds());
      return this.gastosFiltradosToolbar()
        .map((gasto) => {
          const categoria = this.obtenerNombreCategoria(gasto) || 'Otros';
          const { nombre } = this.parseNotas(gasto.notas, categoria);
          const fecha = this.parseFechaTransaccion(gasto.fechaRegistro ?? gasto.fechaTransaccion);
          const estaPagado = pagadosMarcados.has(gasto.id) || this.estaMarcadoComoPagado(gasto.notas);
          return {
            id: gasto.id,
            nombre,
            categoria,
            fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
            monto: Number(gasto.monto || 0),
            estado: estaPagado ? 'Pagado' as const : 'Pendiente' as const,
            fechaOrden: fecha.getTime(),
          };
        })
        .sort((a, b) => b.fechaOrden - a.fechaOrden)
        .map(({ fechaOrden, ...fila }) => fila);
    });

  constructor() {
    this.stateService.cargarDatos();
    if (typeof window !== 'undefined' && window.localStorage.getItem(this.tourStorageKey) !== '1') {
      const empezar = () => setTimeout(() => this.iniciarTour(), 150);
      if (document.readyState === 'complete') {
        empezar();
      } else {
        window.addEventListener('load', empezar, { once: true });
      }
    }
  }

  iniciarTour(): void {
    this.tourPasoIndex.set(0);
    this.tourAbierto.set(true);
    this.actualizarTourObjetivo(true);
  }

  omitirTour(): void {
    this.tourAbierto.set(false);
    this.tourRect.set(null);
    this.tourCardRect.set(null);
    if (this.tourSyncTimer) {
      clearTimeout(this.tourSyncTimer);
      this.tourSyncTimer = null;
    }
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(this.tourStorageKey, '1');
    }
  }

  siguienteTour(): void {
    if (this.tourEsUltimoPaso()) {
      this.omitirTour();
      return;
    }
    this.tourPasoIndex.update((idx) => idx + 1);
    this.actualizarTourObjetivo(true);
  }

  seleccionarTab(tab: 'todos' | 'pagados' | 'pendientes' | 'recurrentes'): void {
    this.tabActiva.set(tab);
  }

  actualizarBusqueda(valor: string): void {
    this.terminoBusqueda.set(valor);
    if (!valor.trim()) {
      this.seleccionExportacionIds.set([]);
    }
  }

  alternarFiltros(): void {
    this.filtrosAbiertos.set(!this.filtrosAbiertos());
  }

  limpiarFiltrosToolbar(): void {
    this.terminoBusqueda.set('');
    this.filtroNombre.set('');
    this.filtroCategoriaId.set('');
    this.filtroFechaDesde.set('');
    this.filtroFechaHasta.set('');
    this.filtroMontoMin.set('');
    this.filtroMontoMax.set('');
    this.seleccionExportacionIds.set([]);
  }

  alternarSeleccionExportacion(id: string, seleccionado: boolean): void {
    this.seleccionExportacionIds.update((ids) => {
      if (seleccionado) {
        return ids.includes(id) ? ids : [...ids, id];
      }
      return ids.filter((item) => item !== id);
    });
  }

  seleccionarTodasCoincidenciasBusqueda(): void {
    this.seleccionExportacionIds.set(this.coincidenciasBusqueda().map((item) => item.id));
  }

  limpiarSeleccionExportacion(): void {
    this.seleccionExportacionIds.set([]);
  }

  exportarExcel(): void {
    const filas = this.construirFilasExportacion(this.idsExportacionSeleccionados());
    if (!filas.length) return;

    const encabezados = ['Nombre', 'Categoria', 'Fecha', 'Monto', 'Metodo de pago', 'Estado'];
    const contenido = [
      encabezados.join(';'),
      ...filas.map((fila) =>
        [
          fila.nombre,
          fila.categoria,
          fila.fecha,
          fila.monto.toFixed(2),
          fila.metodoPago,
          fila.estado,
        ]
          .map((valor) => `"${String(valor).replace(/"/g, '""')}"`)
          .join(';')
      ),
    ].join('\n');

    const blob = new Blob([`\uFEFF${contenido}`], { type: 'text/csv;charset=utf-8;' });
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(blob);
    enlace.download = `gastos-${this.obtenerFechaLocal()}.csv`;
    enlace.click();
    URL.revokeObjectURL(enlace.href);
  }

  exportarGastoExcel(id: string): void {
    const filas = this.construirFilasExportacion(new Set([id]));
    if (!filas.length) return;

    const fila = filas[0];
    const encabezados = ['Nombre', 'Categoria', 'Fecha', 'Monto', 'Metodo de pago', 'Estado'];
    const contenido = [
      encabezados.join(';'),
      [
        fila.nombre,
        fila.categoria,
        fila.fecha,
        fila.monto.toFixed(2),
        fila.metodoPago,
        fila.estado,
      ]
        .map((valor) => `"${String(valor).replace(/"/g, '""')}"`)
        .join(';'),
    ].join('\n');

    const blob = new Blob([`\uFEFF${contenido}`], { type: 'text/csv;charset=utf-8;' });
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(blob);
    enlace.download = `gasto-${id}.csv`;
    enlace.click();
    URL.revokeObjectURL(enlace.href);
  }

  exportarPdf(): void {
    const filas = this.construirFilasExportacion(this.idsExportacionSeleccionados());
    if (!filas.length) return;

    const tabla = filas
      .map(
        (fila) => `
          <tr>
            <td>${fila.nombre}</td>
            <td>${fila.categoria}</td>
            <td>${fila.fecha}</td>
            <td>S/ ${fila.monto.toFixed(2)}</td>
            <td>${fila.metodoPago}</td>
            <td>${fila.estado}</td>
          </tr>`
      )
      .join('');

    const ventana = window.open('', '_blank', 'width=1000,height=700');
    if (!ventana) return;

    ventana.document.write(`
      <html>
        <head>
          <title>Reporte de gastos</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 24px; color: #111827; }
            h1 { margin: 0 0 12px; font-size: 20px; }
            p { margin: 0 0 20px; color: #4b5563; }
            table { width: 100%; border-collapse: collapse; }
            th, td { border: 1px solid #d1d5db; padding: 8px; font-size: 12px; text-align: left; }
            th { background: #f3f4f6; }
          </style>
        </head>
        <body>
          <h1>Reporte de gastos</h1>
          <p>Fecha de exportación: ${new Date().toLocaleString('es-PE')}</p>
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Categoría</th>
                <th>Fecha</th>
                <th>Monto</th>
                <th>Método</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>${tabla}</tbody>
          </table>
        </body>
      </html>
    `);
    ventana.document.close();
    ventana.focus();
    ventana.print();
  }

  exportarGastoPdf(id: string): void {
    const filas = this.construirFilasExportacion(new Set([id]));
    if (!filas.length) return;
    const fila = filas[0];
    const ventana = window.open('', '_blank', 'width=900,height=650');
    if (!ventana) return;

    ventana.document.write(`
      <html>
        <head>
          <title>Gasto ${id}</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 24px; color: #111827; }
            h1 { margin: 0 0 12px; font-size: 20px; }
            table { width: 100%; border-collapse: collapse; }
            th, td { border: 1px solid #d1d5db; padding: 8px; font-size: 12px; text-align: left; }
            th { background: #f3f4f6; width: 180px; }
          </style>
        </head>
        <body>
          <h1>Detalle de gasto</h1>
          <table>
            <tr><th>Nombre</th><td>${fila.nombre}</td></tr>
            <tr><th>Categoría</th><td>${fila.categoria}</td></tr>
            <tr><th>Fecha</th><td>${fila.fecha}</td></tr>
            <tr><th>Monto</th><td>S/ ${fila.monto.toFixed(2)}</td></tr>
            <tr><th>Método</th><td>${fila.metodoPago}</td></tr>
            <tr><th>Estado</th><td>${fila.estado}</td></tr>
          </table>
        </body>
      </html>
    `);
    ventana.document.close();
    ventana.focus();
    ventana.print();
  }

  marcarPendienteComoPagado(id: string): void {
    const pendiente = this.gastosPendientes().find((p) => p.id === id);
    if (!pendiente) return;

    this.pendientesPagadosIds.update((ids) => (ids.includes(id) ? ids : [id, ...ids]));
    this.stateService.gastos.update((gastos) =>
      gastos.map((gasto) =>
        gasto.id === id
          ? { ...gasto, notas: this.anexarMarcadorPagado(gasto.notas) }
          : gasto
      )
    );

    this.transaccionesService.obtenerPorId(id).subscribe({
      next: (tx) => {
        const request: TransaccionRequestDTO = {
          usuarioId: tx.usuarioId,
          nombreCliente: tx.nombreCliente,
          monto: tx.monto,
          tipo: tx.tipo,
          categoriaId: tx.categoriaId,
          fechaTransaccion: tx.fechaTransaccion,
          metodoPago: tx.metodoPago,
          notas: this.anexarMarcadorPagado(tx.notas),
          descripcion: tx.descripcion ?? '',
          etiquetas: tx.etiquetas ?? '',
        };
        this.transaccionesService.actualizar(id, request).subscribe({
          next: () => {
            this.stateService.invalidarCache();
            this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
          },
          error: () => this.mensajeFormulario.set('No se pudo actualizar el estado del pendiente.'),
        });
      },
      error: () => this.mensajeFormulario.set('No se pudo obtener la transacción pendiente.'),
    });
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
    
    // Encontrar ID de categoría a partir de filas o por nombre como fallback
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

    this.transaccionesService.eliminar(id).subscribe({
      next: () => {
        this.gastoPendienteEliminar.set(null);
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
      },
      error: () => this.mensajeFormulario.set('No se pudo eliminar el gasto.'),
    });
  }

  cancelarEliminarGasto(): void {
    this.gastoPendienteEliminar.set(null);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  abrirSelectorFecha(): void {
    const input = this.fechaInput?.nativeElement;
    if (!input) {
      return;
    }

    const pickerInput = input as HTMLInputElement & { showPicker?: () => void };
    if (typeof pickerInput.showPicker === 'function') {
      pickerInput.showPicker();
      return;
    }

    input.focus();
  }

  guardarGasto(): void {
    const errores = this.validarFormulario();
    this.errores.set(errores);
    this.mensajeFormulario.set('');

    if (Object.keys(errores).length > 0) {
      return;
    }

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
      const usuarioIdEdit = this.authService.usuario()?.id;
      if (!usuarioIdEdit) {
        this.mensajeFormulario.set('No se encontró sesión activa.');
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
        notas: `${this.nombreGasto().trim()}|${this.descripcion().trim()}|${this.tipoFrecuencia()}`,
        descripcion: this.descripcion().trim(),
        etiquetas: this.etiquetas().join(','),
      };

      this.guardandoGasto.set(true);
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

    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      this.mensajeFormulario.set('No se encontró sesión activa.');
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
      notas: `${this.nombreGasto().trim()}|${this.descripcion().trim()}|${this.tipoFrecuencia()}`,
      descripcion: this.descripcion().trim(),
      etiquetas: this.etiquetas().join(','),
    };

    this.guardandoGasto.set(true);
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
    this.iaMensaje.set('');
    if (!d || d.length < 4) {
      this.sugerenciasIa.set([]);
      this.iaMensaje.set('Escribe al menos 4 caracteres para sugerir categoría.');
      return;
    }
    if (this.clasificandoIa()) return;
    this.clasificandoIa.set(true);

    this.iaService.getClasificarTransaccion({
      id_temporal: 'nuevo-gasto',
      tipo_movimiento: 'GASTO',
      descripcion: d,
      etiquetas: this.etiquetas().join(',')
    }).subscribe({
      next: (res) => {
        this.clasificandoIa.set(false);
        const sugerencias = res?.datos?.sugerencias ?? [];
        this.sugerenciasIa.set(sugerencias);
        if (!sugerencias.length) {
          this.iaMensaje.set('La IA no devolvió sugerencias para esta descripción.');
        }
      },
      error: () => {
        this.clasificandoIa.set(false);
        this.sugerenciasIa.set([]);
        this.iaMensaje.set('No se pudo obtener sugerencias de IA en este momento.');
      }
    });
  }

  actualizarDescripcion(valor: string): void {
    const texto = valor ?? '';
    const palabras = texto.trim().split(/\s+/).filter(Boolean);
    if (palabras.length <= 200) {
      this.descripcion.set(texto);
      return;
    }
    this.descripcion.set(palabras.slice(0, 200).join(' '));
  }

  agregarEtiqueta(): void {
    const raw = this.nuevaEtiqueta().trim();
    if (!raw) return;
    const tag = raw.split(' ')[0];
    if (!this.etiquetas().includes(tag)) {
      this.etiquetas.update(tags => [...tags, tag]);
    }
    this.nuevaEtiqueta.set('');
  }

  eliminarEtiqueta(tag: string): void {
    this.etiquetas.update(tags => tags.filter(t => t !== tag));
  }

  confirmarCrearCategoriaGasto(nombre: string): void {
    const nameTrim = nombre.trim();
    if (!nameTrim) return;

    const match = this.categoriasDisponibles.find(
      c => c.nombre.toLowerCase() === nameTrim.toLowerCase()
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
        this.stateService.categorias.update(cats => [...cats, cat]);
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
    } else if (this.contarPalabras(this.descripcion()) > 200) {
      out['descripcion'] = 'La descripción permite máximo 200 palabras.';
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
    this.fecha.set(this.obtenerFechaLocal());
    this.metodoPago.set('DIGITAL');
    this.tipoFrecuencia.set('DIARIO');
    this.etiquetas.set([]);
    this.nuevaEtiqueta.set('');
    this.errores.set({});
    this.mensajeFormulario.set('');
    this.sugerenciasIa.set([]);
    this.iaMensaje.set('');
  }

  private contarPalabras(texto: string): number {
    return texto.trim().split(/\s+/).filter(Boolean).length;
  }

  private actualizarTourObjetivo(scroll: boolean): void {
    if (!this.tourAbierto()) return;
    const paso = this.tourPasoActual();
    if (!paso) return;
    if (this.tourSyncTimer) {
      clearTimeout(this.tourSyncTimer);
      this.tourSyncTimer = null;
    }
    const el = document.querySelector(`[data-tour-id="${paso.targetId}"]`) as HTMLElement | null;

    if (!el) {
      this.tourRect.set({
        top: Math.max(20, (window.innerHeight / 2) - 80),
        left: Math.max(20, (window.innerWidth / 2) - 180),
        width: 360,
        height: 160,
      });
      this.tourCardRect.set({
        top: Math.max(16, (window.innerHeight / 2) - 110),
        left: Math.max(12, (window.innerWidth / 2) - 200),
        width: Math.min(420, window.innerWidth - 24),
      });
      return;
    }

    const syncRect = () => {
      if (!this.tourAbierto() || this.tourPasoActual()?.id !== paso.id) return;
      const r = el.getBoundingClientRect();

      if (r.width === 0 && r.height === 0) {
        requestAnimationFrame(syncRect);
        return;
      }

      const cardWidth = Math.min(420, window.innerWidth - 24);
      const cardHeightApprox = 240;
      const margin = 14;
      const minPad = 12;
      const maxLeft = window.innerWidth - cardWidth - minPad;
      let cardLeft = r.right + margin;
      if (cardLeft > maxLeft) {
        cardLeft = r.left - cardWidth - margin;
      }
      if (cardLeft < minPad) {
        cardLeft = Math.max(minPad, r.left);
      }
      cardLeft = Math.min(Math.max(minPad, cardLeft), maxLeft);
      let cardTop = r.bottom + margin;
      if (cardTop + cardHeightApprox > window.innerHeight - minPad) {
        cardTop = r.top - cardHeightApprox - margin;
      }
      if (cardTop < minPad) {
        cardTop = minPad;
      }
      cardTop = Math.min(Math.max(minPad, cardTop), Math.max(minPad, window.innerHeight - cardHeightApprox - minPad));

      this.tourRect.set({
        top: Math.max(8, r.top - 8),
        left: Math.max(8, r.left - 8),
        width: r.width + 16,
        height: r.height + 16,
      });
      this.tourCardRect.set({
        top: cardTop,
        left: cardLeft,
        width: cardWidth,
      });
    };

    if (scroll) {
      el.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'nearest' });
    }

    const delay = scroll ? 220 : 0;
    this.tourSyncTimer = setTimeout(() => {
      requestAnimationFrame(() => requestAnimationFrame(syncRect));
    }, delay);
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  onTourViewportChange(): void {
    if (this.tourAbierto()) {
      this.actualizarTourObjetivo(false);
    }
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

  private parseNotasMetadata(notas: string | null, fallbackName: string): { nombre: string; frecuencia: string } {
    if (!notas) {
      return { nombre: fallbackName, frecuencia: 'MENSUAL' };
    }
    const partes = notas.split('|');
    const nombre = partes[0]?.trim() || fallbackName;
    const frecuencia = partes[2]?.trim() || 'MENSUAL';
    return { nombre, frecuencia };
  }

  fechaChat(fechaTexto: string): string {
    const fecha = this.parseFechaFila(fechaTexto);
    const hoy = new Date();
    const esHoy =
      fecha.getDate() === hoy.getDate() &&
      fecha.getMonth() === hoy.getMonth() &&
      fecha.getFullYear() === hoy.getFullYear();
    return esHoy ? 'Hoy' : fechaTexto;
  }

  fechaRelativa(fecha: Date): string {
    const hoy = this.inicioDelDia(new Date());
    const valor = this.inicioDelDia(fecha);
    const diff = Math.round((hoy.getTime() - valor.getTime()) / 86400000);
    if (diff <= 0) return 'Hoy';
    if (diff === 1) return 'Ayer';
    return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short' });
  }

  verHistorialCompleto(): void {
    this.tabActiva.set('todos');
    const destino = document.querySelector('.gastos-page__analytics');
    if (destino instanceof HTMLElement) {
      destino.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  gastoEmoji(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('comida') || key.includes('alimento')) return '🍔';
    if (key.includes('transport')) return '🚕';
    if (key.includes('entreten')) return '🎮';
    if (key.includes('educa') || key.includes('salud')) return '🎓';
    return '☕';
  }

  gastoEmojiClase(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('comida') || key.includes('alimento')) return 'is-food';
    if (key.includes('transport')) return 'is-transport';
    if (key.includes('entreten')) return 'is-fun';
    if (key.includes('educa') || key.includes('salud')) return 'is-study';
    return 'is-coffee';
  }

  private parseFechaFila(fechaTexto: string): Date {
    if (fechaTexto === 'Hoy') return new Date();
    const normalizada = fechaTexto
      .toLowerCase()
      .replace('.', '')
      .replace('ene', 'jan')
      .replace('abr', 'apr')
      .replace('ago', 'aug')
      .replace('dic', 'dec');
    const dt = new Date(normalizada);
    if (!Number.isNaN(dt.getTime())) return dt;
    return new Date();
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

  private normalizarFrecuencia(valor: string): 'MENSUAL' | 'SEMANAL' | 'QUINCENAL' | 'ANUAL' | 'DIARIO' {
    const v = valor.toUpperCase();
    if (v.includes('ANUAL') || v.includes('AÑO') || v.includes('ANO')) return 'ANUAL';
    if (v.includes('DIARIO')) return 'DIARIO';
    if (v.includes('SEMANAL')) return 'SEMANAL';
    if (v.includes('QUINCENAL')) return 'QUINCENAL';
    return 'MENSUAL';
  }

  private parseFechaFlexible(fechaTexto: string): Date | null {
    if (!fechaTexto) return null;
    const dt = new Date(fechaTexto);
    if (!Number.isNaN(dt.getTime())) return dt;

    const normalizada = fechaTexto
      .toLowerCase()
      .replace('.', '')
      .replace('ene', 'jan')
      .replace('feb', 'feb')
      .replace('mar', 'mar')
      .replace('abr', 'apr')
      .replace('may', 'may')
      .replace('jun', 'jun')
      .replace('jul', 'jul')
      .replace('ago', 'aug')
      .replace('set', 'sep')
      .replace('oct', 'oct')
      .replace('nov', 'nov')
      .replace('dic', 'dec');

    const normalizadaDate = new Date(normalizada);
    if (!Number.isNaN(normalizadaDate.getTime())) return normalizadaDate;

    const match = fechaTexto.match(/^(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?$/);
    if (!match) return null;
    const day = Number(match[1]);
    const month = Number(match[2]) - 1;
    const year = match[3] ? Number(match[3].length === 2 ? `20${match[3]}` : match[3]) : new Date().getFullYear();
    const parsed = new Date(year, month, day);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  private formatearFechaRecordatorio(fechaTexto: string): string {
    const fecha = this.parseFechaFlexible(fechaTexto);
    if (!fecha) return fechaTexto;
    const dia = String(fecha.getDate()).padStart(2, '0');
    const mes = fecha.toLocaleDateString('es-PE', { month: 'long' });
    return `${dia} ${mes.charAt(0).toUpperCase()}${mes.slice(1)}`;
  }

  private calcularDiasParaFecha(fechaTexto: string): number | null {
    const fecha = this.parseFechaFlexible(fechaTexto);
    if (!fecha) return null;
    const hoy = new Date();
    const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    const objetivo = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
    return Math.ceil((objetivo.getTime() - inicioHoy.getTime()) / 86400000);
  }

  private cargarGastos(): void {
    this.stateService.cargarDatos();
  }

  private calcularVariacion(actual: number, previo: number): number {
    if (!previo) return 0;
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

  private obtenerNombreCategoria(gasto: TransaccionDTO): string {
    return gasto.categoriaNombre ?? gasto.categoria ?? 'Otros';
  }

  private obtenerFechaLocal(): string {
    const hoy = new Date();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${hoy.getFullYear()}-${mes}-${dia}`;
  }

  abrirCalendarioVencimientos(): void {
    if (this.calendarioVencimientosAbierto()) {
      this.cerrarCalendarioVencimientos();
      return;
    }

    this.fechaCalendarioSeleccionada.set(null);
    if (!this.calendarioVencimientosAbierto()) {
      const proximo = this.proximoVencimiento();
      this.mesCalendario.set(proximo ? new Date(proximo.fechaOrden) : new Date());
    }
    this.calendarioVencimientosAbierto.set(true);
  }

  cerrarCalendarioVencimientos(): void {
    this.calendarioVencimientosAbierto.set(false);
    this.fechaCalendarioSeleccionada.set(null);
  }

  cambiarMesCalendario(desplazamiento: number): void {
    const actual = this.mesCalendario();
    this.mesCalendario.set(new Date(actual.getFullYear(), actual.getMonth() + desplazamiento, 1));
  }

  seleccionarDiaCalendario(fechaISO: string): void {
    this.fechaCalendarioSeleccionada.set(fechaISO);
    this.mesCalendario.set(this.parseFechaIsoLocal(fechaISO));
  }

  private mesAnterior(): Date {
    const hoy = new Date();
    return new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
  }

  private pendientesDelMes(base: Date): Array<{ monto: number }> {
    const mes = base.getMonth();
    const anio = base.getFullYear();
    return this.gastosPendientes().filter((p) => {
      const fecha = this.parseFechaTransaccion(p.fechaOrden);
      return fecha.getMonth() === mes && fecha.getFullYear() === anio;
    });
  }

  private esFechaProxima(fecha: Date): boolean {
    const hoy = new Date();
    const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    const objetivo = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
    const dias = Math.ceil((objetivo.getTime() - inicioHoy.getTime()) / 86400000);
    return dias >= 0 && dias <= 3;
  }

  private parseFechaTransaccion(valor: number | string): Date {
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime()) ? new Date() : fecha;
  }

  private parseFechaIsoLocal(valor: string): Date {
    const [anio, mes, dia] = valor.split('-').map(Number);
    if (!anio || !mes || !dia) {
      return new Date(valor);
    }
    return new Date(anio, mes - 1, dia);
  }

  private construirCalendarioMes(base: Date): Array<
    | { tipo: 'vacio' }
    | {
        tipo: 'dia';
        dia: number;
        fechaISO: string;
        tieneVencimientos: boolean;
        cantidad: number;
        esHoy: boolean;
        esSeleccionado: boolean;
      }
  > {
    const anio = base.getFullYear();
    const mes = base.getMonth();
    const primerDia = new Date(anio, mes, 1);
    const inicioSemana = (primerDia.getDay() + 6) % 7;
    const totalDias = new Date(anio, mes + 1, 0).getDate();
    const seleccion = this.fechaCalendarioSeleccionada();
    const seleccionFecha = seleccion ? this.parseFechaIsoLocal(seleccion) : null;
    const hoy = new Date();
    const inicioHoy = this.inicioDelDia(hoy);
    const vencimientosPorDia = new Map<string, number>();

    for (const pendiente of this.gastosPendientes()) {
      const fecha = this.parseFechaTransaccion(pendiente.fechaOrden);
      if (this.inicioDelDia(fecha).getTime() < inicioHoy.getTime()) continue;
      if (fecha.getFullYear() !== anio || fecha.getMonth() !== mes) continue;
      const key = this.claveFecha(fecha);
      vencimientosPorDia.set(key, (vencimientosPorDia.get(key) ?? 0) + 1);
    }

    const salida: Array<
      | { tipo: 'vacio' }
      | {
          tipo: 'dia';
          dia: number;
          fechaISO: string;
          tieneVencimientos: boolean;
          cantidad: number;
          esHoy: boolean;
          esSeleccionado: boolean;
        }
    > = [];

    for (let i = 0; i < inicioSemana; i++) {
      salida.push({ tipo: 'vacio' });
    }

    for (let dia = 1; dia <= totalDias; dia++) {
      const fecha = new Date(anio, mes, dia);
      const key = this.claveFecha(fecha);
      salida.push({
        tipo: 'dia',
        dia,
        fechaISO: key,
        tieneVencimientos: (vencimientosPorDia.get(key) ?? 0) > 0,
        cantidad: vencimientosPorDia.get(key) ?? 0,
        esHoy: this.inicioDelDia(fecha).getTime() === inicioHoy.getTime(),
        esSeleccionado: seleccionFecha ? this.esMismaFecha(fecha, seleccionFecha) : false,
      });
    }

    return salida;
  }

  private inicioDelDia(fecha: Date): Date {
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  }

  private claveFecha(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  private esMismaFecha(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  private estaMarcadoComoPagado(notas?: string | null): boolean {
    return (notas ?? '').toUpperCase().split('|').includes('PAGADO');
  }

  private anexarMarcadorPagado(notas?: string | null): string {
    const base = (notas ?? '').trim();
    if (this.estaMarcadoComoPagado(base)) return base;
    return base ? `${base}|PAGADO` : 'PAGADO';
  }

  private construirFilasExportacion(idsSeleccionados: Set<string> | null): Array<{
    nombre: string;
    categoria: string;
    fecha: string;
    monto: number;
    metodoPago: string;
    estado: string;
  }> {
    const pagadosMarcados = new Set(this.pendientesPagadosIds());
    const gastosFuente = idsSeleccionados
      ? this.gastos().filter((gasto) => idsSeleccionados.has(gasto.id))
      : this.gastos();

    return gastosFuente
      .map((gasto) => {
        const categoria = this.obtenerNombreCategoria(gasto) || 'Otros';
        const { nombre } = this.parseNotas(gasto.notas, categoria);
        const fecha = this.parseFechaTransaccion(gasto.fechaRegistro ?? gasto.fechaTransaccion);
        const estaPagado = pagadosMarcados.has(gasto.id) || this.estaMarcadoComoPagado(gasto.notas);
        return {
          nombre,
          categoria,
          fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }),
          monto: Number(gasto.monto || 0),
          metodoPago: gasto.metodoPago || 'DIGITAL',
          estado: estaPagado ? 'Pagado' : 'Pendiente',
          fechaOrden: fecha.getTime(),
        };
      })
      .sort((a, b) => b.fechaOrden - a.fechaOrden)
      .map(({ fechaOrden, ...fila }) => fila);
  }

  private idsExportacionSeleccionados(): Set<string> | null {
    const ids = this.seleccionExportacionIds();
    if (!ids.length) return null;
    const existentes = new Set(this.gastos().map((gasto) => gasto.id));
    const validos = ids.filter((id) => existentes.has(id));
    return validos.length ? new Set(validos) : null;
  }

  private iconoCategoriaEmoji(categoria: string, nombre: string): string {
    const texto = `${categoria} ${nombre}`.toLowerCase();
    if (texto.includes('comida')) return '🍔';
    if (texto.includes('netflix')) return '🎬';
    if (texto.includes('spotify')) return '🎵';
    if (texto.includes('internet')) return '🌐';
    if (texto.includes('luz')) return '⚡';
    if (texto.includes('agua')) return '💧';
    if (texto.includes('gasolina')) return '⛽';
    if (texto.includes('gas')) return '🔥';
    if (texto.includes('taxi')) return '🚕';
    if (texto.includes('bus')) return '🚌';
    if (texto.includes('farmacia')) return '💊';
    if (texto.includes('salud')) return '🏥';
    if (texto.includes('universidad')) return '🎓';
    if (texto.includes('celular')) return '📱';
    if (texto.includes('compra')) return '🛒';
    return '🧾';
  }

  private formaPagoVisual(metodo: string): string {
    const m = metodo.toUpperCase();
    if (m.includes('TARJETA')) return '💳 Tarjeta';
    if (m.includes('TRANSFER')) return '🏦 Cuenta bancaria';
    if (m.includes('EFECTIVO')) return '💵 Efectivo';
    return '📱 Yape/Plin';
  }

  private calcularDiasRestantes(fechaObjetivo: Date): number | null {
    if (Number.isNaN(fechaObjetivo.getTime())) return null;
    const hoy = new Date();
    const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    const objetivo = new Date(fechaObjetivo.getFullYear(), fechaObjetivo.getMonth(), fechaObjetivo.getDate());
    return Math.ceil((objetivo.getTime() - inicioHoy.getTime()) / 86400000);
  }

  private textoDiasRestantes(dias: number | null): string {
    if (dias === null) return '⏰ Fecha pendiente';
    if (dias > 1) return `⏰ En ${dias} días`;
    if (dias === 1) return '⏰ Mañana';
    if (dias === 0) return '🔴 Vence hoy';
    return `🔴 Hace ${Math.abs(dias)} días`;
  }

  private claseDiasRestantes(dias: number | null): string {
    if (dias === null) return 'is-warning';
    if (dias > 7) return 'is-success';
    if (dias >= 3) return 'is-warning';
    if (dias === 1) return 'is-orange';
    return 'is-danger';
  }

  private calcularEstadoPendiente(dias: number | null): 'Programado' | 'Pendiente' | 'Vencido' {
    if (dias === null) return 'Pendiente';
    if (dias < 0) return 'Vencido';
    if (dias <= 3) return 'Pendiente';
    return 'Programado';
  }

  private claseEstadoPendiente(estado: 'Programado' | 'Pendiente' | 'Vencido'): string {
    if (estado === 'Programado') return 'is-programado';
    if (estado === 'Pendiente') return 'is-pendiente';
    return 'is-vencido';
  }

  private calcularPrioridad(dias: number | null): 'Alta' | 'Media' | 'Baja' {
    if (dias === null) return 'Media';
    if (dias <= 1) return 'Alta';
    if (dias <= 7) return 'Media';
    return 'Baja';
  }

  private clasePrioridad(prioridad: 'Alta' | 'Media' | 'Baja'): string {
    if (prioridad === 'Alta') return 'is-high';
    if (prioridad === 'Media') return 'is-medium';
    return 'is-low';
  }

  private progresoAnual(fechaVencimiento: Date): number {
    const hoy = new Date();
    const fin = new Date(fechaVencimiento);
    const inicio = new Date(fechaVencimiento);
    inicio.setFullYear(fin.getFullYear() - 1);
    const total = Math.max(fin.getTime() - inicio.getTime(), 1);
    const transcurrido = Math.min(Math.max(hoy.getTime() - inicio.getTime(), 0), total);
    return Math.round((transcurrido / total) * 100);
  }

}

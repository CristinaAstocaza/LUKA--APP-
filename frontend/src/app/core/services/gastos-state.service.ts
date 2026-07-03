import { Injectable, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Transacciones } from './transacciones';
import { FinancieroService } from './Financiero.service';
import { TransaccionDTO } from '../models/financiero/transaccion.model';
import { ResumenFinancieroDTO } from '../models/financiero/resumen.model';
import { CategoriaDTO } from '../models/financiero/categoria.model';

@Injectable({
  providedIn: 'root'
})
export class GastosStateService {
  // ── Angular Signals for State ──
  readonly gastos = signal<TransaccionDTO[]>([]);
  readonly resumenActual = signal<ResumenFinancieroDTO | null>(null);
  readonly resumenAnterior = signal<ResumenFinancieroDTO | null>(null);
  readonly categorias = signal<CategoriaDTO[]>([]);

  // ── Loading state ──
  readonly cargando = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Cache timestamps (ms)
  private ultimoRefrescoTransacciones = 0;
  private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes for transaction/current month summary
  private readonly usuarioDemoId = 'local-demo-user';

  constructor(
    private transaccionesService: Transacciones,
    private financieroService: FinancieroService
  ) {}

  /**
   * Loads all required data for the Gastos section.
   * Leverages caching for static datasets (previous month and categories) and time-based TTL for volatile data.
   */
  cargarDatos(forzar: boolean = false): void {
    const ahora = Date.now();
    const necesitaRefrescoVolatil = forzar || !this.ultimoRefrescoTransacciones || (ahora - this.ultimoRefrescoTransacciones > this.CACHE_TTL_MS);

    const necesitaCategorias = this.categorias().length === 0;
    const necesitaResumenAnterior = this.resumenAnterior() === null;

    if (!necesitaRefrescoVolatil && !necesitaCategorias && !necesitaResumenAnterior) {
      // Everything is already cached
      return;
    }

    this.cargando.set(true);
    this.error.set(null);

    const hoy = new Date();
    const mesActual = hoy.getMonth() + 1;
    const anioActual = hoy.getFullYear();
    const anterior = new Date(anioActual, hoy.getMonth() - 1, 1);
    const mesAnterior = anterior.getMonth() + 1;
    const anioAnterior = anterior.getFullYear();

    // Build calls conditionally
    const llamadas: Record<string, any> = {};

    if (necesitaRefrescoVolatil) {
      llamadas['historial'] = this.transaccionesService.listarHistorialCompleto({ tipo: 'GASTO', tamanio: 100 }).pipe(
        catchError(() => of([]))
      );
      llamadas['resumenActual'] = this.financieroService.getResumen(mesActual, anioActual).pipe(
        catchError(() => of(null))
      );
    } else {
      llamadas['historial'] = of(null);
      llamadas['resumenActual'] = of(null);
    }

    if (necesitaResumenAnterior) {
      llamadas['resumenAnterior'] = this.financieroService.getResumen(mesAnterior, anioAnterior).pipe(
        catchError(() => of(null))
      );
    } else {
      llamadas['resumenAnterior'] = of(null);
    }

    if (necesitaCategorias) {
      llamadas['categorias'] = this.financieroService.getCategorias('GASTO').pipe(
        catchError(() => of([]))
      );
    } else {
      llamadas['categorias'] = of(null);
    }

    forkJoin(llamadas).subscribe({
      next: (res: any) => {
        const historialBackend: TransaccionDTO[] = Array.isArray(res.historial) ? res.historial : [];
        const usarDatosLocales =
          historialBackend.length === 0 &&
          this.gastos().length === 0 &&
          !res.resumenActual &&
          !res.resumenAnterior &&
          (!Array.isArray(res.categorias) || res.categorias.length === 0);

        const gastosFuente = usarDatosLocales ? this.crearGastosDemo() : historialBackend;
        const resumenActualFuente = usarDatosLocales
          ? this.calcularResumenMensual(gastosFuente, new Date())
          : res.resumenActual;
        const resumenAnteriorFuente = usarDatosLocales
          ? this.calcularResumenMensual(gastosFuente, new Date(new Date().getFullYear(), new Date().getMonth() - 1, 1))
          : res.resumenAnterior;
        const categoriasFuente = usarDatosLocales ? this.crearCategoriasDemo() : res.categorias;

        if (res.historial !== null || usarDatosLocales) {
          this.gastos.set(gastosFuente || []);
          this.ultimoRefrescoTransacciones = Date.now();
        }
        if (res.resumenActual !== null || usarDatosLocales) {
          this.resumenActual.set(resumenActualFuente ?? null);
        }
        if (res.resumenAnterior !== null || usarDatosLocales) {
          this.resumenAnterior.set(resumenAnteriorFuente ?? null);
        }
        if (res.categorias !== null || usarDatosLocales) {
          this.categorias.set(categoriasFuente || []);
        }
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('[GastosStateService] Error loading page data:', err);
        this.error.set('Error al sincronizar datos financieros.');
        this.cargando.set(false);
      }
    });
  }

  /**
   * Invalidates caches and forces a clean refetch.
   */
  invalidarCache(): void {
    this.ultimoRefrescoTransacciones = 0;
    this.cargarDatos(true);
  }

  private crearCategoriasDemo(): CategoriaDTO[] {
    return [
      { id: 'cat-comida', nombre: 'Comida', descripcion: 'Alimentación', icono: 'utensils', tipo: 'GASTO' },
      { id: 'cat-transporte', nombre: 'Transporte', descripcion: 'Movilidad', icono: 'bus', tipo: 'GASTO' },
      { id: 'cat-servicios', nombre: 'Servicios', descripcion: 'Servicios del hogar', icono: 'wifi', tipo: 'GASTO' },
      { id: 'cat-hogar', nombre: 'Hogar', descripcion: 'Compras del hogar', icono: 'house', tipo: 'GASTO' },
      { id: 'cat-salud', nombre: 'Salud', descripcion: 'Gastos médicos', icono: 'briefcase-medical', tipo: 'GASTO' },
      { id: 'cat-entretenimiento', nombre: 'Entretenimiento', descripcion: 'Ocio', icono: 'film', tipo: 'GASTO' },
    ];
  }

  private crearGastosDemo(): TransaccionDTO[] {
    const hoy = new Date();
    const base = [
      this.crearGastoDemo('demo-001', 'Almuerzo oficina', 22.5, 'cat-comida', 'Comida', -1, 'DIGITAL'),
      this.crearGastoDemo('demo-002', 'Taxi trabajo', 18, 'cat-transporte', 'Transporte', -2, 'DIGITAL'),
      this.crearGastoDemo('demo-003', 'Mercado semanal', 130, 'cat-hogar', 'Hogar', -4, 'TARJETA'),
      this.crearGastoDemo('demo-004', 'Internet hogar', 89.9, 'cat-servicios', 'Servicios', -5, 'TRANSFERENCIA'),
      this.crearGastoDemo('demo-005', 'Farmacia', 46.4, 'cat-salud', 'Salud', -7, 'DIGITAL'),
      this.crearGastoDemo('demo-006', 'Streaming', 34.9, 'cat-entretenimiento', 'Entretenimiento', -9, 'TARJETA'),
      this.crearGastoDemo('demo-007', 'Gasolina', 95, 'cat-transporte', 'Transporte', -11, 'TARJETA'),
      this.crearGastoDemo('demo-008', 'Cena familiar', 120, 'cat-comida', 'Comida', -14, 'DIGITAL'),
      this.crearGastoDemo('demo-009', 'Luz', 78.6, 'cat-servicios', 'Servicios', -18, 'TRANSFERENCIA'),
      this.crearGastoDemo('demo-010', 'Agua', 42.2, 'cat-servicios', 'Servicios', -24, 'TRANSFERENCIA'),
      this.crearGastoDemo('demo-011', 'Pasajes', 28, 'cat-transporte', 'Transporte', -27, 'EFECTIVO'),
      this.crearGastoDemo('demo-012', 'Supermercado', 168, 'cat-hogar', 'Hogar', -32, 'TARJETA'),
      this.crearGastoDemo('demo-013', 'Consulta médica', 70, 'cat-salud', 'Salud', -38, 'DIGITAL'),
      this.crearGastoDemo('demo-014', 'Cine', 32, 'cat-entretenimiento', 'Entretenimiento', -45, 'DIGITAL'),
      this.crearGastoDemo('demo-015', 'Comida rápida', 19, 'cat-comida', 'Comida', -54, 'EFECTIVO'),
      this.crearGastoDemo('demo-016', 'Mantenimiento hogar', 110, 'cat-hogar', 'Hogar', -63, 'TRANSFERENCIA'),
      this.crearGastoDemo('demo-017', 'Taxi noche', 24, 'cat-transporte', 'Transporte', -72, 'DIGITAL'),
      this.crearGastoDemo('demo-018', 'Plataforma música', 18.9, 'cat-entretenimiento', 'Entretenimiento', -88, 'TARJETA'),
      this.crearGastoDemo('demo-019', 'Internet próximo corte', 89.9, 'cat-servicios', 'Servicios', 2, 'TRANSFERENCIA'),
      this.crearGastoDemo('demo-020', 'Tarjeta crédito', 240, 'cat-hogar', 'Hogar', 5, 'DIGITAL'),
    ];

    return base.sort(
      (a, b) => new Date(b.fechaTransaccion).getTime() - new Date(a.fechaTransaccion).getTime()
    );
  }

  private crearGastoDemo(
    id: string,
    nombre: string,
    monto: number,
    categoriaId: string,
    categoriaNombre: string,
    desfaseDias: number,
    metodoPago: TransaccionDTO['metodoPago']
  ): TransaccionDTO {
    const fecha = new Date();
    fecha.setHours(10, 0, 0, 0);
    fecha.setDate(fecha.getDate() + desfaseDias);
    const fechaIso = fecha.toISOString();

    return {
      id,
      usuarioId: this.usuarioDemoId,
      nombreCliente: 'Usuario local',
      monto,
      tipo: 'GASTO',
      categoriaId,
      categoria: categoriaNombre,
      categoriaNombre,
      categoriaIcono: '',
      fechaTransaccion: fechaIso,
      metodoPago,
      etiquetas: null,
      notas: `${nombre}|${categoriaNombre}|RECURRENTE`,
      descripcion: `${nombre} (dato local)`,
      fechaRegistro: fechaIso,
    };
  }

  private calcularResumenMensual(gastos: TransaccionDTO[], base: Date): ResumenFinancieroDTO {
    const anio = base.getFullYear();
    const mes = base.getMonth();
    const desde = new Date(anio, mes, 1, 0, 0, 0, 0);
    const hasta = new Date(anio, mes + 1, 0, 23, 59, 59, 999);
    const gastosMes = gastos.filter((g) => {
      const fecha = new Date(g.fechaTransaccion);
      return fecha >= desde && fecha <= hasta;
    });

    const totalGastos = gastosMes.reduce((acc, g) => acc + Number(g.monto || 0), 0);
    const cantidadGastos = gastosMes.length;

    return {
      desde: desde.toISOString(),
      hasta: hasta.toISOString(),
      totalIngresos: 0,
      totalGastos,
      balance: -totalGastos,
      cantidadIngresos: 0,
      cantidadGastos,
      totalTransacciones: cantidadGastos,
      promedioIngreso: 0,
      promedioGasto: cantidadGastos ? totalGastos / cantidadGastos : 0,
    };
  }
}

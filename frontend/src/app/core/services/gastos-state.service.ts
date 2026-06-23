import { Injectable, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { Transacciones } from './transacciones';
import { FinancieroService } from './Financiero.service';
import { TransaccionDTO, TransaccionFiltros } from '../models/financiero/transaccion.model';
import { ResumenFinancieroDTO } from '../models/financiero/resumen.model';
import { CategoriaDTO } from '../models/financiero/categoria.model';

@Injectable({
  providedIn: 'root'
})
export class GastosStateService {
  // ── Angular Signals for State ──
  readonly gastos = signal<TransaccionDTO[]>([]);
  readonly gastosMesAnterior = signal<TransaccionDTO[]>([]);
  readonly resumenActual = signal<ResumenFinancieroDTO | null>(null);
  readonly resumenAnterior = signal<ResumenFinancieroDTO | null>(null);
  readonly categorias = signal<CategoriaDTO[]>([]);

  // ── Loading state ──
  readonly cargando = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Cache timestamps (ms)
  private ultimoRefrescoTransacciones = 0;
  private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes for transaction/current month summary

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
      llamadas['historial'] = this.cargarHistorialCompleto({ tipo: 'GASTO', mes: mesActual, anio: anioActual });
      llamadas['historialAnterior'] = this.cargarHistorialCompleto({ tipo: 'GASTO', mes: mesAnterior, anio: anioAnterior });
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
        if (res.historial !== null) {
          this.gastos.set(res.historial || []);
          this.ultimoRefrescoTransacciones = Date.now();
        }
        if (res.historialAnterior !== null) {
          this.gastosMesAnterior.set(res.historialAnterior || []);
        }
        if (res.resumenActual !== null) {
          this.resumenActual.set(res.resumenActual);
        }
        if (res.resumenAnterior !== null) {
          this.resumenAnterior.set(res.resumenAnterior);
        }
        if (res.categorias !== null) {
          this.categorias.set(res.categorias || []);
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

  private cargarHistorialCompleto(filtros: Partial<TransaccionFiltros>): any {
    const base: Partial<TransaccionFiltros> = {
      pagina: 0,
      tamanio: 100,
      ...filtros
    };

    return this.transaccionesService.listarHistorial(base).pipe(
      switchMap((primeraPagina) => {
        const totalPaginas = primeraPagina.totalPages || 1;
        if (totalPaginas <= 1) {
          return of(primeraPagina.content || []);
        }

        const peticiones = Array.from({ length: totalPaginas - 1 }, (_, idx) =>
          this.transaccionesService.listarHistorial({
            ...base,
            pagina: idx + 1,
            tamanio: 100
          })
        );

        return forkJoin(peticiones).pipe(
          map((paginas) => [
            ...(primeraPagina.content || []),
            ...paginas.flatMap((pagina) => pagina.content || [])
          ])
        );
      }),
      catchError(() => of([]))
    );
  }
}

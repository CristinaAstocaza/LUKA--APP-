import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { EMPTY, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { expand, reduce } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import { AuthService } from './auth.service';
import {
  TransaccionDTO,
  TransaccionApiDTO,
  TransaccionRequestDTO,
  TransaccionFiltros,
} from '../models/financiero/transaccion.model';
import { PaginaDTO } from '../models/shared/pagina.model';
import { ResultadoApi } from '../models/auth/user.model';

@Injectable({ providedIn: 'root' })
export class Transacciones {

  private base = `${environment.gatewayUrl}/api/v1/financiero/transacciones`;

  constructor(private http: HttpClient, private auth: AuthService) {}

  private normalizarTransaccion(transaccion: TransaccionApiDTO): TransaccionDTO {
    const categoria = transaccion.categoria ?? transaccion.categoriaNombre ?? '';

    return {
      ...transaccion,
      categoria,
      categoriaNombre: transaccion.categoriaNombre ?? categoria,
      categoriaIcono: transaccion.categoriaIcono ?? '',
      etiquetas: transaccion.etiquetas ?? null,
      notas: transaccion.notas ?? null,
      descripcion: transaccion.descripcion ?? null,
      fechaRegistro: transaccion.fechaRegistro ?? transaccion.fechaTransaccion,
    };
  }

  private normalizarLista(transacciones: TransaccionApiDTO[] = []): TransaccionDTO[] {
    return transacciones.map((transaccion) => this.normalizarTransaccion(transaccion));
  }

  /* Registrar una transacción (ingreso o gasto) */
  registrar(request: TransaccionRequestDTO): Observable<TransaccionDTO> {
    return this.http.post<ResultadoApi<TransaccionApiDTO>>(this.base, request).pipe(
      map((res) => this.normalizarTransaccion(res.datos))
    );
  }

  /* Registrar varias transacciones a la vez */
  registrarLote(requests: TransaccionRequestDTO[]): Observable<TransaccionDTO[]> {
    return this.http.post<ResultadoApi<TransaccionApiDTO[]>>(`${this.base}/lote`, requests).pipe(
      map((res) => this.normalizarLista(res.datos))
    );
  }

  /**
   * Historial paginado con filtros opcionales.
   * — Módulo Gastos:
   *     listarHistorial({ tipo: 'GASTO' })
   * — Módulo Ingresos:
   *     listarHistorial({ tipo: 'INGRESO' })
   * — Dashboard (todos del mes):
   *     listarHistorial({ mes: 5, anio: 2025 })
   */
  listarHistorial(filtros: Partial<TransaccionFiltros> = {}): Observable<PaginaDTO<TransaccionDTO>> {
    const usuarioId = filtros.usuarioId ?? this.auth.usuario()?.id ?? '';
    let params = new HttpParams().set('usuarioId', usuarioId);

    if (filtros.tipo)         params = params.set('tipo',        filtros.tipo);
    if (filtros.categoriaId)  params = params.set('categoriaId', filtros.categoriaId);
    if (filtros.mes  != null) params = params.set('mes',         filtros.mes);
    if (filtros.anio != null) params = params.set('anio',        filtros.anio);
    params = params
      .set('pagina',  filtros.pagina  ?? 0)
      .set('tamanio', filtros.tamanio ?? 20);

    return this.http.get<ResultadoApi<TransaccionApiDTO[]>>(`${this.base}/historial`, { params }).pipe(
      map(resp => {
        const content = this.normalizarLista(resp.datos);
        const pag = resp.pagina;
        return {
          content: content,
          totalElements: pag ? pag.totalElementos : content.length,
          totalPages: pag ? pag.totalPaginas : 1,
          number: pag ? pag.numeroPagina : 0,
          size: pag ? pag.tamañoPagina : content.length
        } as PaginaDTO<TransaccionDTO>;
      })
    );
  }

  listarHistorialCompleto(filtros: Partial<TransaccionFiltros> = {}): Observable<TransaccionDTO[]> {
    const tamanio = filtros.tamanio ?? 100;
    const baseFiltros: Partial<TransaccionFiltros> = {
      ...filtros,
      pagina: 0,
      tamanio,
    };

    return this.listarHistorial(baseFiltros).pipe(
      expand((pagina) => {
        if (pagina.number + 1 >= pagina.totalPages) {
          return EMPTY;
        }

        return this.listarHistorial({
          ...baseFiltros,
          pagina: pagina.number + 1,
        });
      }),
      map((pagina) => pagina.content),
      reduce((acumulado, content) => acumulado.concat(content), [] as TransaccionDTO[])
    );
  }

  /* Detalle de una transacción por ID */
  obtenerPorId(id: string): Observable<TransaccionDTO> {
    return this.http.get<ResultadoApi<TransaccionApiDTO>>(`${this.base}/${id}`).pipe(
      map((res) => this.normalizarTransaccion(res.datos))
    );
  }

  /* Actualizar transacción */
  actualizar(id: string, request: TransaccionRequestDTO): Observable<TransaccionDTO> {
    return this.http.put<ResultadoApi<TransaccionApiDTO>>(`${this.base}/${id}`, request).pipe(
      map((res) => this.normalizarTransaccion(res.datos))
    );
  }

  /* Eliminar transacción */
  eliminar(id: string): Observable<void> {
    return this.http.delete<ResultadoApi<void>>(`${this.base}/${id}`).pipe(
      map(() => undefined)
    );
  }
}

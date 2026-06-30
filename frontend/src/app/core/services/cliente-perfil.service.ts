import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import {
  RespuestaDatosPersonales,
  RespuestaPerfilFinanciero,
  SolicitudDatosPersonales,
  SolicitudPerfilFinanciero
} from '../models/cliente/perfil-cliente.model';
import { ResultadoApi } from '../models/auth/user.model';

@Injectable({ providedIn: 'root' })
export class ClientePerfilService {
  private basePerfil = `${environment.gatewayUrl}/api/v1/clientes/perfil`;
  private basePerfilFinanciero = `${environment.gatewayUrl}/api/v1/clientes/perfil-financiero`;

  constructor(private http: HttpClient) {}

  crearPerfilInicial(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.http.post<ResultadoApi<RespuestaDatosPersonales>>(`${this.basePerfil}/inicial?usuarioId=${usuarioId}`, {}).pipe(
      map(res => res.datos)
    );
  }

  consultarPerfil(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.http.get<ResultadoApi<RespuestaDatosPersonales>>(`${this.basePerfil}/${usuarioId}`).pipe(
      map(res => res.datos)
    );
  }

  obtenerPerfil(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.consultarPerfil(usuarioId);
  }

  actualizarPerfil(usuarioId: string, payload: SolicitudDatosPersonales): Observable<RespuestaDatosPersonales> {
    if (usuarioId === '00000000-0000-0000-0000-000000000000') {
      const mockResponse: RespuestaDatosPersonales = {
        dni: payload.dni,
        nombres: payload.nombres,
        apellidos: payload.apellidos,
        genero: payload.genero,
        edad: payload.edad,
        telefono: payload.telefono,
        fotoPerfilUrl: '',
        pais: payload.pais || '',
        ciudad: payload.ciudad || '',
        datosCompletos: true,
        fechaCreacion: new Date().toISOString(),
        fechaActualizacion: new Date().toISOString(),
        fechaNacimiento: payload.fechaNacimiento
      };
      localStorage.setItem('luka_mocked_perfil_test', JSON.stringify(mockResponse));
      return of(mockResponse);
    }

    return this.http.put<ResultadoApi<RespuestaDatosPersonales>>(`${this.basePerfil}/${usuarioId}`, payload).pipe(
      map(res => res.datos)
    );
  }

  eliminarCuenta(usuarioId: string): Observable<void> {
    return this.http.delete<ResultadoApi<void>>(`${this.basePerfil}/${usuarioId}`).pipe(
      map(() => undefined)
    );
  }

  consultarPerfilFinanciero(usuarioId: string): Observable<RespuestaPerfilFinanciero> {
    return this.http.get<ResultadoApi<RespuestaPerfilFinanciero>>(`${this.basePerfilFinanciero}/${usuarioId}`).pipe(
      map(res => res.datos)
    );
  }

  guardarPerfilFinanciero(usuarioId: string, payload: SolicitudPerfilFinanciero): Observable<RespuestaPerfilFinanciero> {
    return this.http.put<ResultadoApi<RespuestaPerfilFinanciero>>(`${this.basePerfilFinanciero}/${usuarioId}`, payload).pipe(
      map(res => res.datos)
    );
  }
}

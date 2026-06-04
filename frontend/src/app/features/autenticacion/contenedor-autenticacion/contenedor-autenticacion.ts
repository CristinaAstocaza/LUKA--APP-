import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta, RegistroExitosoPayload } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';
import { AuthService } from '../../../core/services/auth.service';
import { TipoCanalOtp } from '../../../core/models/auth/user.model';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion implements OnInit {
  vistaActual: 'login' | 'registro' | 'canal' | 'verificar' | 'exito' = 'login';

  /** Datos del registro para el paso de verificación */
  medioVerificacion: 'correo' | 'celular' = 'correo';
  destinoVerificacion = '';
  registroPendiente: RegistroExitosoPayload | null = null;
  canalSeleccionado: TipoCanalOtp = 'EMAIL';
  cargandoOtp = false;
  errorOtp = '';
  mensajeOtp = '';

  constructor(private route: ActivatedRoute, private authService: AuthService) {}

  ngOnInit(): void {
    // Leer el estado inicial desde los datos de la ruta
    this.route.data.subscribe(data => {
      if (data['vista']) {
        this.vistaActual = data['vista'];
      }
    });
  }

  cambiarVista(vista: 'login' | 'registro'): void {
    this.vistaActual = vista;
  }

  /** Maneja el registro exitoso y muestra la verificación de código */
  onRegistroExitoso(datos: RegistroExitosoPayload): void {
    this.registroPendiente = datos;
    this.canalSeleccionado = datos.telefono ? 'SMS' : 'EMAIL';
    this.errorOtp = '';
    this.mensajeOtp = 'Elige el canal y solicita tu código de verificación.';
    this.vistaActual = 'canal';
  }

  onCuentaNoActivada(datos: { correo: string }): void {
    this.registroPendiente = {
      usuarioId: '',
      correo: datos.correo,
      telefono: null,
      medio: 'correo',
      destino: datos.correo
    };
    this.canalSeleccionado = 'EMAIL';
    this.errorOtp = '';
    this.mensajeOtp = 'Tu cuenta está pendiente de activación. Solicita un nuevo código para continuar.';
    this.vistaActual = 'canal';
  }

  solicitarCodigoActivacion(): void {
    if (!this.registroPendiente) return;

    this.cargandoOtp = true;
    this.errorOtp = '';
    this.mensajeOtp = '';

    this.authService.solicitarOtpActivacion({
      email: this.registroPendiente.correo,
      telefono: this.canalSeleccionado === 'EMAIL' ? null : this.registroPendiente.telefono,
      tipo: this.canalSeleccionado
    }).subscribe({
      next: (mensaje) => {
        this.cargandoOtp = false;
        this.mensajeOtp = mensaje;
        this.medioVerificacion = this.canalSeleccionado === 'EMAIL' ? 'correo' : 'celular';
        this.destinoVerificacion = this.canalSeleccionado === 'EMAIL'
          ? this.registroPendiente!.correo
          : this.registroPendiente!.telefono ?? '';
        this.vistaActual = 'verificar';
      },
      error: (err) => {
        this.cargandoOtp = false;
        this.errorOtp = err.error?.mensaje ?? err.error?.error ?? 'No se pudo enviar el código de verificación.';
      }
    });
  }

  /** Maneja la verificación exitosa del código */
  onCodigoVerificado(codigo: string): void {
    if (!this.registroPendiente) return;

    this.authService.activarCuenta(
      this.registroPendiente.correo,
      codigo,
      this.canalSeleccionado === 'EMAIL' ? null : this.registroPendiente.telefono
    ).subscribe({
      next: () => {
        this.vistaActual = 'exito';
      },
      error: (err) => {
        this.errorOtp = err.error?.mensaje ?? err.error?.error ?? 'No se pudo activar la cuenta con el código ingresado.';
      }
    });
  }
}

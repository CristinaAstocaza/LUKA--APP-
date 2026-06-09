import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';
import { AuthService } from '../../../core/services/auth.service';

export type VistaAuth = 'login' | 'registro' | 'verificar' | 'exito' | 'canal';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion implements OnInit {
  vistaActual: VistaAuth = 'login';

  cargandoOtp = false;
  infoOtp: string | null = null;
  errorOtp: string | null = null;

  correoActivacion = '';
  telefonoActivacion = '';
  usuarioId = '';

  canalSeleccionado: 'EMAIL' | 'SMS' | 'WHATSAPP' = 'EMAIL';

  medioVerificacion: 'correo' | 'celular' = 'correo';
  destinoVerificacion = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      if (data['vista']) {
        this.vistaActual = data['vista'] as VistaAuth;
      }
    });
  }

  cambiarVista(vista: 'login' | 'registro'): void {
    this.vistaActual = vista;

    const ruta =
      vista === 'login'
        ? '/autenticacion/iniciar-sesion'
        : '/autenticacion/crear-cuenta';

    this.location.go(ruta);
  }

  onRegistroExitoso(datos: { correo: string; celular?: string; usuarioId: string }): void {
    this.correoActivacion = datos.correo;
    this.telefonoActivacion = datos.celular || '';
    this.usuarioId = datos.usuarioId;
    this.canalSeleccionado = 'EMAIL';
    this.vistaActual = 'canal';
  }

  seleccionarCanal(canal: 'EMAIL' | 'SMS' | 'WHATSAPP'): void {
    this.canalSeleccionado = canal;
  }

  enviarCodigoActivacion(): void {
    this.cargandoOtp = true;
    this.infoOtp = 'Enviando código de verificación...';
    this.errorOtp = null;

    const payload = {
      email: this.correoActivacion,
      tipo: this.canalSeleccionado,
      telefono:
        this.canalSeleccionado !== 'EMAIL'
          ? this.telefonoActivacion
          : undefined,
    };

    this.authService.solicitarOtpActivacion(payload).subscribe({
      next: resp => {
        this.cargandoOtp = false;

        if (resp.exito) {
          this.infoOtp = resp.mensaje || 'Código enviado correctamente';

          this.medioVerificacion =
            this.canalSeleccionado === 'EMAIL' ? 'correo' : 'celular';

          this.destinoVerificacion =
            this.canalSeleccionado === 'EMAIL'
              ? this.correoActivacion
              : this.telefonoActivacion;

          this.vistaActual = 'verificar';
        } else {
          this.infoOtp = null;
          this.errorOtp =
            resp.mensaje || 'Error al enviar el código de verificación';
        }
      },
      error: err => {
        this.cargandoOtp = false;
        this.infoOtp = null;
        this.errorOtp =
          err.error?.mensaje || 'Error de conexión con el servidor';

        console.error('Error al enviar OTP:', err);
      },
    });
  }

  onCodigoVerificado(codigo: string): void {
    console.log('Cuenta verificada con código:', codigo);
    this.vistaActual = 'exito';
  }
}
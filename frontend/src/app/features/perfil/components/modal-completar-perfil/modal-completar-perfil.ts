import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { ClientePerfilService } from '../../../../core/services/cliente-perfil.service';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { SolicitudDatosPersonales } from '../../../../core/models/cliente/perfil-cliente.model';

@Component({
  selector: 'app-modal-completar-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './modal-completar-perfil.html',
  styleUrl: './modal-completar-perfil.scss'
})
export class ModalCompletarPerfil implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly perfilService = inject(ClientePerfilService);
  private readonly dashboardState = inject(DashboardStateService);

  perfilForm!: FormGroup;
  
  // States
  readonly cargandoDni = signal(false);
  readonly guardando = signal(false);
  readonly edadCalculada = signal<number | null>(null);
  readonly errorMsg = signal<string | null>(null);
  readonly correoUsuario = signal<string>('');

  // Phone OTP verification states
  readonly cargandoOtp = signal(false);
  readonly telefonoEnviado = signal(false);
  readonly telefonoVerificado = signal(false);
  readonly errorOtp = signal('');

  ngOnInit(): void {
    const usuario = this.authService.usuario();
    this.correoUsuario.set(usuario?.nombreUsuario || '');

    this.perfilForm = this.fb.group({
      correo: [{ value: this.correoUsuario(), disabled: true }],
      dni: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      telefono: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
      genero: ['', [Validators.required]],
      pais: ['', [Validators.required, Validators.maxLength(20)]],
      ciudad: ['', [Validators.required, Validators.maxLength(100)]],
      fechaNacimiento: ['', [Validators.required]]
    });

    // Listen to DNI input to simulate external API
    this.perfilForm.get('dni')?.valueChanges.subscribe(val => {
      if (val && val.length === 8 && /^[0-9]{8}$/.test(val)) {
        this.simularConsultaDNI(val);
      }
    });

    // Listen to birth date to calculate age
    this.perfilForm.get('fechaNacimiento')?.valueChanges.subscribe(val => {
      this.actualizarEdad(val);
    });
  }

  // Simulación de API DNI (como RENIEC de Perú)
  simularConsultaDNI(dni: string): void {
    this.cargandoDni.set(true);
    this.errorMsg.set(null);

    // Mock API delay
    setTimeout(() => {
      this.cargandoDni.set(false);
      
      // Simular respuestas de ejemplo
      if (dni === '00000000') {
        this.errorMsg.set('DNI no encontrado o inválido.');
        return;
      }

      // Autocomplete random names depending on DNI digits to simulate search
      const seed = parseInt(dni.substring(5), 10) || 123;
      const nombresMock = [
        'Juan Carlos', 'María Fernanda', 'Luis Alberto', 'Ana Beatriz', 
        'Jorge Eduardo', 'Sofía Irene', 'Carlos Miguel', 'Diana Patricia'
      ];
      const apellidosMock = [
        'Pérez Rodríguez', 'Gómez Sánchez', 'Quispe Mamani', 'Flores Díaz',
        'Vargas Llosa', 'Mendoza Castillo', 'Alvarado Ruiz', 'Romero Cruz'
      ];

      const indexNombres = seed % nombresMock.length;
      const indexApellidos = (seed + 3) % apellidosMock.length;

      this.perfilForm.patchValue({
        nombres: nombresMock[indexNombres],
        apellidos: apellidosMock[indexApellidos]
      });
    }, 1000);
  }

  // Calculate age automatically on date change
  actualizarEdad(fechaStr: string): void {
    if (!fechaStr) {
      this.edadCalculada.set(null);
      return;
    }

    const nacimiento = new Date(fechaStr);
    const hoy = new Date();
    
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mes = hoy.getMonth() - nacimiento.getMonth();
    
    if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }

    this.edadCalculada.set(edad >= 0 ? edad : 0);
  }

  // Solicitar verificación de teléfono simulada (OTP)
  solicitarVerificacionTelefono(): void {
    const tel = this.perfilForm.get('telefono')?.value || '';
    const digitos = tel.replace(/\D/g, '');

    if (digitos.length !== 9) {
      this.perfilForm.get('telefono')?.setErrors({ invalidPeruPhone: true });
      return;
    }

    this.cargandoOtp.set(true);
    this.errorOtp.set('');

    setTimeout(() => {
      this.cargandoOtp.set(false);
      this.telefonoEnviado.set(true);
    }, 800);
  }

  // Validar código OTP simulado (código exitoso: '1234')
  validarCodigoTelefono(codigo: string): void {
    if (codigo.length < 4) {
      this.errorOtp.set('');
      return;
    }

    if (codigo === '1234') {
      this.telefonoVerificado.set(true);
      this.errorOtp.set('');
      this.perfilForm.get('telefono')?.disable();
    } else {
      this.errorOtp.set('El código ingresado es incorrecto. Intente con 1234.');
    }
  }

  onSubmit(): void {
    if (this.perfilForm.invalid || this.guardando()) {
      return;
    }

    const usuario = this.authService.usuario();
    if (!usuario || !usuario.id) {
      this.errorMsg.set('No se pudo identificar la sesión del usuario.');
      return;
    }

    // Validate minimum age
    const edad = this.edadCalculada();
    if (edad === null || edad < 18) {
      this.errorMsg.set('Debes tener al menos 18 años para completar tu perfil.');
      return;
    }

    this.guardando.set(true);
    this.errorMsg.set(null);

    const rawForm = this.perfilForm.getRawValue();
    const payload: SolicitudDatosPersonales = {
      dni: rawForm.dni,
      nombres: rawForm.nombres,
      apellidos: rawForm.apellidos,
      genero: rawForm.genero,
      edad: edad,
      telefono: rawForm.telefono,
      pais: rawForm.pais,
      ciudad: rawForm.ciudad,
      fechaNacimiento: rawForm.fechaNacimiento
    };

    this.perfilService.actualizarPerfil(usuario.id, payload).subscribe({
      next: () => {
        // Refrescar caché del BFF
        this.dashboardState.invalidarCache();
        this.guardando.set(false);
      },
      error: (err) => {
        console.error('Error al guardar datos personales:', err);
        this.errorMsg.set(
          err?.error?.mensaje || 
          err?.error?.error || 
          'Hubo un problema al guardar tus datos. Inténtalo de nuevo.'
        );
        this.guardando.set(false);
      }
    });
  }

  // Get max date allowed (must be 18 years ago)
  get maxFechaNacimiento(): string {
    const hoy = new Date();
    const maxAnio = hoy.getFullYear() - 18;
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${maxAnio}-${mes}-${dia}`;
  }
}

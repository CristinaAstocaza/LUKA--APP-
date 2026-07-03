import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { GastosPage } from './gastos-page';
import { Transacciones } from '../../../../core/services/transacciones';
import { AuthService } from '../../../../core/services/auth.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { IaService } from '../../../../core/services/ia.service';
import { CategoriaDTO } from '../../../../core/models/financiero/categoria.model';
import { ResumenFinancieroDTO } from '../../../../core/models/financiero/resumen.model';
import { TransaccionDTO } from '../../../../core/models/financiero/transaccion.model';

describe('GastosPage', () => {
  let component: GastosPage;
  let fixture: ComponentFixture<GastosPage>;

  const stateServiceMock = {
    gastos: signal([]),
    resumenActual: signal(null),
    resumenAnterior: signal(null),
    categorias: signal([]),
    cargando: signal(false),
    error: signal(null),
    cargarDatos: jasmine.createSpy('cargarDatos'),
    invalidarCache: jasmine.createSpy('invalidarCache'),
  };

  const transaccionesMock = {
    listarHistorial: jasmine.createSpy('listarHistorial').and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 })
    ),
    obtenerPorId: jasmine.createSpy('obtenerPorId').and.returnValue(of({} as TransaccionDTO)),
    registrar: jasmine.createSpy('registrar').and.returnValue(of({} as TransaccionDTO)),
    actualizar: jasmine.createSpy('actualizar').and.returnValue(of({} as TransaccionDTO)),
    eliminar: jasmine.createSpy('eliminar').and.returnValue(of(void 0)),
  };

  const financieroMock = {
    getResumen: jasmine.createSpy('getResumen').and.returnValue(of({} as ResumenFinancieroDTO)),
    getCategorias: jasmine.createSpy('getCategorias').and.returnValue(of([] as CategoriaDTO[])),
    crearCategoria: jasmine.createSpy('crearCategoria').and.returnValue(of({} as CategoriaDTO)),
    resumen: signal<ResumenFinancieroDTO | null>(null),
    categorias: signal<CategoriaDTO[]>([]),
    cargando: signal(false),
  };

  const authMock = {
    usuario: signal({ id: 'user-1', nombreUsuario: 'Usuario', roles: [] }),
  };

  const eventBusMock = {
    emit: jasmine.createSpy('emit'),
  };

  const iaMock = {
    getClasificarTransaccion: jasmine.createSpy('getClasificarTransaccion').and.returnValue(of({ datos: { sugerencias: [] } })),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GastosPage],
      providers: [
        { provide: Transacciones, useValue: transaccionesMock },
        { provide: AuthService, useValue: authMock },
        { provide: FinancieroService, useValue: financieroMock },
        { provide: AppEventBus, useValue: eventBusMock },
        { provide: GastosStateService, useValue: stateServiceMock },
        { provide: IaService, useValue: iaMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GastosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should open the date picker on first click', () => {
    const showPicker = jasmine.createSpy('showPicker');
    (component as any).fechaInput = { nativeElement: { showPicker } };

    component.abrirSelectorFecha();

    expect(showPicker).toHaveBeenCalled();
  });

  it('should default the form date to today when opening the modal', () => {
    component.abrirModal();

    const hoy = new Date();
    const esperado = [
      hoy.getFullYear(),
      String(hoy.getMonth() + 1).padStart(2, '0'),
      String(hoy.getDate()).padStart(2, '0'),
    ].join('-');

    expect(component.fecha()).toBe(esperado);
    expect(component.modalAbierto()).toBeTrue();
  });
});

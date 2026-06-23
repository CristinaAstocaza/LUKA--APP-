import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegistrarGastosPage } from './registrar-gastos-page';

describe('RegistrarGastosPage', () => {
  let component: RegistrarGastosPage;
  let fixture: ComponentFixture<RegistrarGastosPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrarGastosPage],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrarGastosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

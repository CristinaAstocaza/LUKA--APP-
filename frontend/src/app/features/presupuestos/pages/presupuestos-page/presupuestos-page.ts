import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface CategoriaPresupuesto {
  nombre: string;
  asignado: number;
  gastado: number;
}

@Component({
  selector: 'app-presupuestos-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './presupuestos-page.html',
  styleUrls: ['./presupuestos-page.scss']
})
export class PresupuestosPage {

  // PRESUPUESTO GENERAL
  presupuestoTotal: number = 3000;

  // FORMULARIO
  nuevaCategoria: string = '';
  nuevoMonto: number = 0;

  // CATEGORIAS
  categorias: CategoriaPresupuesto[] = [
    {
      nombre: '🍔 Alimentación',
      asignado: 800,
      gastado: 200
    },
    {
      nombre: '🚗 Transporte',
      asignado: 400,
      gastado: 100
    },
    {
      nombre: '📚 Educación',
      asignado: 600,
      gastado: 0
    }
  ];

  // TOTAL ASIGNADO
  get totalAsignado(): number {

    return this.categorias.reduce(
      (total, categoria) =>
        total + categoria.asignado,
      0
    );

  }

  // DISPONIBLE
  get disponible(): number {

    return this.presupuestoTotal - this.totalAsignado;

  }

  // GASTO TOTAL
  get gastoTotal(): number {

    return this.categorias.reduce(
      (total, categoria) =>
        total + categoria.gastado,
      0
    );

  }

  // PORCENTAJE GENERAL
  get porcentajeConsumido(): number {

    if (this.presupuestoTotal <= 0) {
      return 0;
    }

    return Math.round(
      (this.gastoTotal / this.presupuestoTotal) * 100
    );

  }

  // GUARDAR PRESUPUESTO
  guardarPresupuesto(): void {

    alert(
      `Presupuesto guardado: S/. ${this.presupuestoTotal}`
    );

  }

  // AGREGAR CATEGORIA
  agregarCategoria(): void {

    if (
      !this.nuevaCategoria.trim() ||
      this.nuevoMonto <= 0
    ) {

      alert(
        'Ingrese una categoría y un monto válido'
      );

      return;
    }

    if (
      this.totalAsignado + this.nuevoMonto >
      this.presupuestoTotal
    ) {

      alert(
        'El monto supera el presupuesto disponible'
      );

      return;
    }

    this.categorias.push({
      nombre: this.nuevaCategoria,
      asignado: this.nuevoMonto,
      gastado: 0
    });

    this.nuevaCategoria = '';
    this.nuevoMonto = 0;

  }

  // ELIMINAR CATEGORIA
  eliminarCategoria(index: number): void {

    this.categorias.splice(index, 1);

  }

  // CALCULAR PORCENTAJE
  calcularPorcentaje(
    categoria: CategoriaPresupuesto
  ): number {

    if (categoria.asignado <= 0) {
      return 0;
    }

    return Math.round(
      (categoria.gastado / categoria.asignado) * 100
    );

  }

}
import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router,NavigationEnd,ActivatedRoute } from '@angular/router';
import { filter,map } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
interface Breadcrumb {
  label: string,
  route?: string
}

interface Notificacion {
    id:    number;
  icon:  string;
  color: string;
  text:  string;
  time:  string;
  read:  boolean;
}

@Component({
  selector: 'app-header',
  standalone:true,
  imports: [CommonModule,RouterModule],
  templateUrl: './header.html',
  styleUrls: ['./header.scss'],
})
export class Header implements OnInit{

   // ── Página ──
  pageTitle   = 'Resumen';
  breadcrumbs: Breadcrumb[] = [];

  // ── Fecha ──
  currentMonth = this.getMonthLabel();

  // ── Dropdowns ──
  showNotifications = false;
  showUserMenu      = false;

  // ── Notificaciones mock ──
  // TODO: reemplazar con llamada al microservicio de mensajería
  notificaciones: Notificacion[] = [
    {
      id:    1,
      icon:  'fa-solid fa-triangle-exclamation',
      color: '#F59E0B',
      text:  'Estás al 80% de tu presupuesto de comida',
      time:  'Hace 5 min',
      read:  false
    },
    {
      id:    2,
      icon:  'fa-solid fa-circle-check',
      color: '#22C55E',
      text:  '¡Meta de ahorro "Viaje" completada!',
      time:  'Hace 1 hora',
      read:  false
    },
    {
      id:    3,
      icon:  'fa-solid fa-arrow-trend-down',
      color: '#5B6AF0',
      text:  'Tus gastos bajaron un 12% esta semana',
      time:  'Ayer',
      read:  true
    }
  ];

  get notifCount(): number {
    return this.notificaciones.filter(n => !n.read).length;
  }

  constructor(
    public  auth:  AuthService,
    private router: Router
  ) {}

ngOnInit(): void {
  this.router.events
    .pipe(
      filter(e => e instanceof NavigationEnd),
      map(() => {
        let route = this.router.routerState.root;

        while (route.firstChild) {
          route = route.firstChild;
        }

        return route.snapshot.data;
      })
    )
    .subscribe(data => {
      this.pageTitle   = data['title']       ?? 'Luka';
      this.breadcrumbs = data['breadcrumbs'] ?? [];
      this.showNotifications = false;
      this.showUserMenu      = false;
    });

  // Cargar título inicial
  let route = this.router.routerState.root;

  while (route.firstChild) {
    route = route.firstChild;
  }

  const data = route.snapshot.data;
  this.pageTitle   = data['title']       ?? 'Resumen';
  this.breadcrumbs = data['breadcrumbs'] ?? [];
}
  // ── Cierra dropdowns al hacer clic fuera ──
  @HostListener('document:click')
  onDocumentClick(): void {
    this.showNotifications = false;
    this.showUserMenu      = false;
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    this.showUserMenu      = false;
  }

  toggleUserMenu(): void {
    this.showUserMenu      = !this.showUserMenu;
    this.showNotifications = false;
  }

  markAllRead(): void {
    this.notificaciones.forEach(n => (n.read = true));
  }

  openQuickAdd(): void {
    // TODO: abrir modal de gasto rápido
    console.log('Abrir modal rápido');
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  private getMonthLabel(): string {
    return new Date().toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
  }

}


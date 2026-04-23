import { Routes } from '@angular/router';
import { Layout } from './layout/layout/layout/layout';
import { DashboardPage } from './features/dashboard/pages/dashboard-page/dashboard-page';

export const routes: Routes = [
  {
    path: 'dashboard',
    component: DashboardPage
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
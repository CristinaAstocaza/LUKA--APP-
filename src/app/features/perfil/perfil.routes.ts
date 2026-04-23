import { Routes } from "@angular/router";
import { PerfilPage } from "./pages/perfil-page/perfil-page";

export const PERFIL_ROUTES: Routes = [

{
    path: '',
    component: PerfilPage,
    data: {
        title: 'Perfil',
        breadcrumbs: [
            {label: 'Perfil', route: '/perfil'},
            {label: 'Sección'}
        ]
    }
}

]
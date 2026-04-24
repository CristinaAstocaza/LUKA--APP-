import { Routes } from "@angular/router";
import { AyudaPage } from "../ayuda/pages/ayuda-page/ayuda-page";

export const CONFIGURACION_ROUTES: Routes = [

{
    path: '',
    component: AyudaPage,
    data: {
        title: 'Configuracion',
        breadcrumbs: [
            {label: 'Configuracion', route: '/configuracion'},
            {label: 'Sección'}
        ]
    }
}

]
import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page').then((module) => module.LoginPage),
    title: 'Ingresar | EsthetiAdmin',
  },
  {
    // Public booking page (ADR 014): no login and outside the panel's layout.
    path: 'turnos',
    loadComponent: () =>
      import('./features/booking/pages/booking-page/booking-page').then((module) => module.BookingPage),
    title: 'Sacá tu turno | EsthetiAdmin',
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/main-layout/main-layout').then((module) => module.MainLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/home/pages/home-page/home-page').then((module) => module.HomePage),
        title: 'Inicio | EsthetiAdmin',
      },
      {
        path: 'agenda',
        loadComponent: () =>
          import('./features/appointments/pages/agenda-page/agenda-page').then((module) => module.AgendaPage),
        title: 'Agenda | EsthetiAdmin',
      },
      {
        path: 'clientes',
        loadComponent: () =>
          import('./features/clients/pages/clients-page/clients-page').then((module) => module.ClientsPage),
        title: 'Clientes | EsthetiAdmin',
      },
      {
        path: 'clientes/:id',
        loadComponent: () =>
          import('./features/clients/pages/client-detail-page/client-detail-page').then(
            (module) => module.ClientDetailPage,
          ),
        title: 'Ficha del cliente | EsthetiAdmin',
      },
      {
        path: 'tratamientos',
        loadComponent: () =>
          import('./features/treatments/pages/treatments-page/treatments-page').then(
            (module) => module.TreatmentsPage,
          ),
        title: 'Tratamientos | EsthetiAdmin',
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./features/reports/pages/reports-page/reports-page').then((module) => module.ReportsPage),
        title: 'Reportes | EsthetiAdmin',
      },
      {
        path: 'servicios',
        loadComponent: () =>
          import('./features/catalog/pages/catalog-page/catalog-page').then((module) => module.CatalogPage),
        title: 'Servicios | EsthetiAdmin',
      },
      {
        path: 'horarios',
        loadComponent: () =>
          import('./features/availability/pages/availability-page/availability-page').then(
            (module) => module.AvailabilityPage,
          ),
        title: 'Horarios | EsthetiAdmin',
      },
      {
        path: 'configuracion',
        loadComponent: () =>
          import('./features/settings/pages/settings-page/settings-page').then((module) => module.SettingsPage),
        title: 'Configuración | EsthetiAdmin',
      },
      {
        path: 'gastos',
        loadComponent: () =>
          import('./features/expenses/pages/expenses-page/expenses-page').then((module) => module.ExpensesPage),
        title: 'Gastos | EsthetiAdmin',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

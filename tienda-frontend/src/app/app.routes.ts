import { Routes } from '@angular/router';
import { adminGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Ingresar | La Cuevita',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    title: 'Crear cuenta | La Cuevita',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'admin',
    title: 'Panel | La Cuevita',
    canActivate: [adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('./features/store/store.routes').then((m) => m.STORE_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
import { Routes } from '@angular/router';

export const LEGAL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./legal-layout/legal-layout.component').then((m) => m.LegalLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'terminos' },
      {
        path: 'terminos',
        title: 'Términos y condiciones | La Cuevita',
        data: {
          path: 'terminos',
          title: 'Términos y condiciones',
          subtitle: 'Cómo funciona la compra en nuestra tienda.',
          icon: 'gavel',
        },
        loadComponent: () => import('./terms-page/terms-page.component').then((m) => m.TermsPageComponent),
      },
      {
        path: 'privacidad',
        title: 'Política de privacidad | La Cuevita',
        data: {
          path: 'privacidad',
          title: 'Política de privacidad',
          subtitle: 'Qué datos tuyos usamos, para qué y cómo cuidarlos.',
          icon: 'shield',
        },
        loadComponent: () => import('./privacy-page/privacy-page.component').then((m) => m.PrivacyPageComponent),
      },
      {
        path: 'devoluciones',
        title: 'Cambios y devoluciones | La Cuevita',
        data: {
          path: 'devoluciones',
          title: 'Cambios y devoluciones',
          subtitle: 'Qué hacer si tu compra no es lo que esperabas.',
          icon: 'sync_alt',
        },
        loadComponent: () => import('./returns-page/returns-page.component').then((m) => m.ReturnsPageComponent),
      },
      {
        path: 'contacto',
        title: 'Contacto | La Cuevita',
        data: {
          path: 'contacto',
          title: 'Contacto',
          subtitle: 'Estamos para ayudarte con tu compra.',
          icon: 'support_agent',
        },
        loadComponent: () => import('./contact-page/contact-page.component').then((m) => m.ContactPageComponent),
      },
    ],
  },
];

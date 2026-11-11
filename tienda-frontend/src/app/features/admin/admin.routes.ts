import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./layout/admin-layout.component').then((m) => m.AdminLayoutComponent),
        children: [
            { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
            {
                path: 'dashboard',
                loadComponent: () =>
                    import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
            },
            {
                path: 'categorias',
                loadComponent: () =>
                    import('./catalog/categories/categories-page.component').then(
                        (m) => m.CategoriesPageComponent
                    ),
            },
            {
                path: 'productos',
                loadComponent: () =>
                    import('./catalog/products/products-page.component').then(
                        (m) => m.ProductsPageComponent
                    ),
            },
            {
                path: 'productos/:id',
                loadComponent: () =>
                    import('./catalog/products/product-detail-page.component').then(
                        (m) => m.ProductDetailPageComponent
                    ),
            },
            {
                path: 'pedidos',
                title: 'Pedidos | La Cuevita',
                loadComponent: () =>
                    import('./orders/orders-page/orders-page.component').then((m) => m.OrdersPageComponent),
            },
            {
                path: 'pedidos/:id',
                title: 'Pedido | La Cuevita',
                loadComponent: () =>
                    import('./orders/order-detail-page/order-detail-page.component').then(
                        (m) => m.OrderDetailPageComponent
                    ),
            },
            {
                path: 'configuracion',
                title: 'Configuración | La Cuevita',
                loadComponent: () =>
                    import('./settings/settings-page/settings-page.component').then(
                        (m) => m.SettingsPageComponent
                    ),
            },
            {
                path: 'importar',
                title: 'Importar productos | La Cuevita',
                loadComponent: () =>
                    import('./imports/import-page/import-page.component').then(
                        (m) => m.ImportPageComponent
                    ),
            },
            {
                path: 'atributos',
                loadComponent: () =>
                    import('./catalog/attributes/attributes-page.component').then(
                        (m) => m.AttributesPageComponent
                    ),
            },
        ],
    },
];
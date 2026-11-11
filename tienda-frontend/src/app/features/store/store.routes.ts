import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/auth.guard';

export const STORE_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./layout/store-layout/store-layout.component').then((m) => m.StoreLayoutComponent),
        children: [
            {
                path: '',
                pathMatch: 'full',
                title: 'La Cuevita',
                loadComponent: () =>
                    import('./home/home-page/home-page.component').then((m) => m.HomePageComponent),
            },
            {
                path: 'tienda',
                title: 'Tienda | La Cuevita',
                loadComponent: () =>
                    import('./catalog/catalog-page/catalog-page.component').then(
                        (m) => m.CatalogPageComponent
                    ),
            },
            {
                path: 'producto/:id',
                title: 'Producto | La Cuevita',
                loadComponent: () =>
                    import('./product/product-detail-page/product-detail-page.component').then(
                        (m) => m.ProductDetailPageComponent
                    ),
            },
            {
                path: 'pago',
                title: 'Finalizar pedido | La Cuevita',
                canActivate: [authGuard],
                loadComponent: () =>
                    import('./checkout/checkout-page/checkout-page.component').then(
                        (m) => m.CheckoutPageComponent
                    ),
            },
            {
                path: 'pedido/:id',
                title: 'Mi pedido | La Cuevita',
                canActivate: [authGuard],
                loadComponent: () =>
                    import('./orders/order-page/order-page.component').then((m) => m.OrderPageComponent),
            },
            { path: 'cuenta', pathMatch: 'full', redirectTo: 'cuenta/pedidos' },
            {
                path: 'cuenta/pedidos',
                title: 'Mis pedidos | La Cuevita',
                canActivate: [authGuard],
                loadComponent: () =>
                    import('./orders/my-orders-page/my-orders-page.component').then(
                        (m) => m.MyOrdersPageComponent
                    ),
            },
            {
                path: 'carrito',
                title: 'Carrito | La Cuevita',
                loadComponent: () =>
                    import('./cart/cart-page/cart-page.component').then((m) => m.CartPageComponent),
            },
        ],
    },
];
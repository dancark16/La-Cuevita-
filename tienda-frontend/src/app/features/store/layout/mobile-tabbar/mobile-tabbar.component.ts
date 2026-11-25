import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { CartService } from '../../cart/cart.service';
import { StoreApiService } from '../../data/store-api.service';
import { StoreCategory } from '../../data/store.models';

/** Navegación inferior para celular: Inicio, Tienda, Categorías, Carrito y Cuenta. */
@Component({
  selector: 'app-mobile-tabbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './mobile-tabbar.component.html',
  styleUrl: './mobile-tabbar.component.scss',
})
export class MobileTabbarComponent {
  private readonly api = inject(StoreApiService);
  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);

  private readonly all = toSignal(this.api.allCategories(), { initialValue: [] as StoreCategory[] });

  readonly sheetOpen = signal(false);

  readonly groups = computed(() =>
    this.all()
      .filter((category) => category.parentId === null)
      .map((root) => ({ root, children: this.all().filter((category) => category.parentId === root.id) }))
  );

  readonly accountLink = computed(() => (this.auth.isAuthenticated() ? '/cuenta/pedidos' : '/login'));

  closeSheet(): void {
    this.sheetOpen.set(false);
  }
}

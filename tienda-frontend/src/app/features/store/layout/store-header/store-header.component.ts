import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { CartService } from '../../cart/cart.service';
import { StoreApiService } from '../../data/store-api.service';
import { StoreCategory } from '../../data/store.models';
import { BrandLogoComponent } from '../brand-logo/brand-logo.component';

@Component({
  selector: 'app-store-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, BrandLogoComponent],
  templateUrl: './store-header.component.html',
  styleUrl: './store-header.component.scss',
})
export class StoreHeaderComponent {
  private readonly api = inject(StoreApiService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly auth = inject(AuthService);

  readonly cart = inject(CartService);
  private readonly all = toSignal(this.api.allCategories(), { initialValue: [] as StoreCategory[] });

  /** Categorías principales con sus subcategorías, para el menú. */
  readonly groups = computed(() =>
    this.all()
      .filter((category) => category.parentId === null)
      .map((root) => ({ root, children: this.all().filter((category) => category.parentId === root.id) }))
  );
  readonly menuOpen = signal(false);

  readonly accountMenuOpen = signal(false);

  search(event: Event, term: string): void {
    event.preventDefault();
    this.closeMenu();
    this.router.navigate(['/tienda'], { queryParams: { q: term.trim() || null } });
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  toggleAccountMenu(): void {
    this.accountMenuOpen.update((open) => !open);
  }

  logout(destination = '/'): void {
    this.accountMenuOpen.set(false);
    this.closeMenu();
    this.auth.logout();
    this.router.navigate([destination]);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.querySelector('.account')?.contains(event.target as Node)) {
      this.accountMenuOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.accountMenuOpen.set(false);
  }
}

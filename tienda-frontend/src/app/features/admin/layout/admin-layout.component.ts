import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  private readonly auth = inject(AuthService);

  readonly user = this.auth.user;
  readonly menuOpen = signal(false);

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Categorías', icon: 'category', route: '/admin/categorias' },
    { label: 'Pedidos', icon: 'receipt_long', route: '/admin/pedidos' },
    { label: 'Productos', icon: 'checkroom', route: '/admin/productos' },
    { label: 'Importar Excel', icon: 'upload_file', route: '/admin/importar' },
    { label: 'Tallas y colores', icon: 'palette', route: '/admin/atributos' },
    { label: 'Configuración', icon: 'settings', route: '/admin/configuracion' },
  ];

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.auth.logout();
  }
}

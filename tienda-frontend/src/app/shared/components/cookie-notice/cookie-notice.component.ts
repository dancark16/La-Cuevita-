import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

const KEY = 'lacuevita.aviso-navegador';

/** Aviso breve: solo usamos el almacenamiento del navegador para la sesión y el carrito. */
@Component({
  selector: 'app-cookie-notice',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cookie-notice.component.html',
  styleUrl: './cookie-notice.component.scss',
})
export class CookieNoticeComponent {
  readonly visible = signal(this.shouldShow());

  accept(): void {
    try {
      localStorage.setItem(KEY, '1');
    } catch {
      // sin almacenamiento: el aviso volverá a salir, no pasa nada
    }
    this.visible.set(false);
  }

  private shouldShow(): boolean {
    try {
      return localStorage.getItem(KEY) !== '1';
    } catch {
      return true;
    }
  }
}

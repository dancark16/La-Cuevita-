import { Injectable, signal } from '@angular/core';

export type ToastTone = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
  duration: number;
}

let nextId = 1;

/** Avisos del sistema. Los muestra `ToastHostComponent`, que vive en la raíz de la app. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  success(message: string): void {
    this.push('success', message, 3500);
  }

  error(message: string): void {
    this.push('error', message, 7000);
  }

  info(message: string): void {
    this.push('info', message, 4500);
  }

  warning(message: string): void {
    this.push('warning', message, 6000);
  }

  dismiss(id: number): void {
    this._toasts.update((list) => list.filter((toast) => toast.id !== id));
  }

  private push(tone: ToastTone, message: string, duration: number): void {
    const toast: Toast = { id: nextId++, tone, message, duration };
    // Máximo 4 avisos a la vez: los más viejos se van.
    this._toasts.update((list) => [...list, toast].slice(-4));
    setTimeout(() => this.dismiss(toast.id), duration);
  }
}

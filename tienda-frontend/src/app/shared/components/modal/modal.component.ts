import { Component, HostBinding, HostListener, input, output } from '@angular/core';

/**
 * Ventana modal única del sistema: cabecera azul con ícono, cuerpo y pie opcional.
 *
 * - Como ventana propia: `<app-modal title="..." (closed)="...">` dentro de un `@if`.
 * - Dentro de un diálogo de Material (`MatDialog`): `<app-modal [embedded]="true" ...>`; el diálogo
 *   pone el fondo y el foco, y este componente pone todo el aspecto.
 * - Pie con botones: `<div modalFooter>...</div>`.
 */
@Component({
  selector: 'app-modal',
  standalone: true,
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly icon = input('edit');
  /** 'danger' pinta el ícono en rojo (acciones destructivas). */
  readonly tone = input<'default' | 'danger'>('default');
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly embedded = input(false);
  readonly closed = output<void>();

  @HostBinding('class.embedded')
  get isEmbedded(): boolean {
    return this.embedded();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.embedded()) {
      this.closed.emit();
    }
  }
}

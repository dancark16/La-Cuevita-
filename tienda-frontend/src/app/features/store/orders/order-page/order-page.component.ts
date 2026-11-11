import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';
import { BankAccount, StoreSettingsService } from '../../../../core/settings/store-settings.service';
import {
  ORDER_STATUS_LABEL,
  ORDER_STEPS,
  orderStepIndex,
} from '../../../../shared/util/order-status';
import { bankLogo } from '../../../../shared/util/bank-logo';
import { OrderApiService } from '../order-api.service';
import { Order } from '../../../../shared/models/order.model';

@Component({
  selector: 'app-order-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './order-page.component.html',
  styleUrl: './order-page.component.scss',
})
export class OrderPageComponent {
  private readonly api = inject(OrderApiService);

  /** Id del pedido, tomado de la ruta /pedido/:id. */
  readonly id = input.required<string>();

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly uploading = signal(false);
  readonly dragging = signal(false);
  readonly message = signal<{ kind: 'ok' | 'error'; text: string } | null>(null);
  readonly copied = signal<string | null>(null);

  readonly steps = ORDER_STEPS;
  readonly statusLabel = ORDER_STATUS_LABEL;
  private readonly settings = inject(StoreSettingsService);
  readonly logoOf = bankLogo;
  readonly accounts = signal<BankAccount[]>([]);
  readonly hasBank = computed(() => this.accounts().length > 0);

  readonly stepIndex = computed(() => {
    const order = this.order();
    return order ? orderStepIndex(order.status) : -1;
  });

  readonly storeWhatsappUrl = computed(() => {
    const order = this.order();
    const number = this.settings.settings().whatsappNumber;
    if (!order || !number) {
      return null;
    }
    const text = `Hola La Cuevita, soy ${order.customerName}. Ya hice la transferencia del pedido #${order.orderNumber} por $${order.total.toFixed(2)}. Te envío el comprobante.`;
    return `https://wa.me/${number}?text=${encodeURIComponent(text)}`;
  });

  constructor() {
    this.settings.bankAccounts().subscribe((accounts) => this.accounts.set(accounts));

    toObservable(this.id)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.notFound.set(false);
        }),
        switchMap((id) => this.api.get(id).pipe(catchError(() => of(null))))
      )
      .subscribe((order) => {
        this.loading.set(false);
        this.order.set(order);
        this.notFound.set(order === null);
      });
  }

  onChosen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.upload(file);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.upload(file);
    }
  }

  cancel(): void {
    const order = this.order();
    if (!order || !confirm('¿Cancelar este pedido? Esta acción no se puede deshacer.')) {
      return;
    }
    this.api.cancel(order.id).subscribe({
      next: (updated) => this.order.set(updated),
      error: () => this.message.set({ kind: 'error', text: 'No se pudo cancelar el pedido.' }),
    });
  }

  copy(label: string, value: string): void {
    navigator.clipboard?.writeText(value).then(() => {
      this.copied.set(label);
      setTimeout(() => this.copied.set(null), 1500);
    });
  }

  private upload(file: File): void {
    const order = this.order();
    if (!order) {
      return;
    }
    const invalid = this.api.validateReceipt(file);
    if (invalid) {
      this.message.set({ kind: 'error', text: invalid });
      return;
    }
    this.uploading.set(true);
    this.message.set(null);
    this.api.uploadReceipt(order.id, file).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.uploading.set(false);
        this.message.set({ kind: 'ok', text: '¡Comprobante recibido! Lo revisaremos y te avisaremos por WhatsApp.' });
      },
      error: () => {
        this.uploading.set(false);
        this.message.set({ kind: 'error', text: 'No se pudo subir el comprobante. Intenta de nuevo.' });
      },
    });
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ORDER_STATUS_LABEL, OrderStatus, orderStepIndex } from '../../../../shared/util/order-status';
import { OrderApiService } from '../order-api.service';
import { Order } from '../../../../shared/models/order.model';

@Component({
  selector: 'app-my-orders-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './my-orders-page.component.html',
  styleUrl: './my-orders-page.component.scss',
})
export class MyOrdersPageComponent {
  private readonly api = inject(OrderApiService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly statusLabel = ORDER_STATUS_LABEL;

  readonly onTheWay = computed(() => this.orders().filter((o) => o.status === 'SHIPPED').length);
  readonly delivered = computed(() => this.orders().filter((o) => o.status === 'DELIVERED').length);

  /** 'wait' (por pagar), 'ok' (pagado o en camino), 'done' (entregado), 'off' (cancelado). */
  tone(status: OrderStatus): 'wait' | 'ok' | 'done' | 'off' {
    switch (status) {
      case 'PENDING':
        return 'wait';
      case 'DELIVERED':
        return 'done';
      case 'CANCELLED':
        return 'off';
      default:
        return 'ok';
    }
  }

  iconOf(status: OrderStatus): string {
    switch (status) {
      case 'PENDING':
        return 'hourglass_top';
      case 'SHIPPED':
        return 'local_shipping';
      case 'DELIVERED':
        return 'inventory_2';
      case 'CANCELLED':
        return 'block';
      default:
        return 'payments';
    }
  }

  /** Avance del pedido de 25 % (recibido) a 100 % (entregado). */
  progress(status: OrderStatus): number {
    return ((orderStepIndex(status) + 1) / 4) * 100;
  }

  /** Los dos primeros productos y cuántos más hay. */
  summary(order: Order): string {
    const names = order.items.map((item) => item.productName);
    const shown = names.slice(0, 2).join(', ');
    return names.length > 2 ? `${shown} y ${names.length - 2} más` : shown;
  }

  constructor() {
    this.api.mine().subscribe({
      next: (page) => {
        this.orders.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ORDER_STATUS_LABEL } from '../../../../shared/util/order-status';
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

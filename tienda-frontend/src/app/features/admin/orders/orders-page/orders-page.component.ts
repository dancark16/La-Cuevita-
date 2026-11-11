import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { ORDER_STATUS_LABEL, OrderStatus } from '../../../../shared/util/order-status';
import { AdminOrderSummary, AdminOrdersService, OrderStats } from '../admin-orders.service';

interface Tab {
  key: string;
  label: string;
  status: OrderStatus | null;
}

const TABS: Tab[] = [
  { key: 'ALL', label: 'Todos', status: null },
  { key: 'PENDING', label: 'Por confirmar', status: 'PENDING' },
  { key: 'PAID', label: 'Pagados', status: 'PAID' },
  { key: 'SHIPPED', label: 'Enviados', status: 'SHIPPED' },
  { key: 'DELIVERED', label: 'Entregados', status: 'DELIVERED' },
  { key: 'CANCELLED', label: 'Cancelados', status: 'CANCELLED' },
];

const PAGE_SIZE = 20;

@Component({
  selector: 'app-orders-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './orders-page.component.html',
  styleUrl: './orders-page.component.scss',
})
export class OrdersPageComponent implements OnInit {
  private readonly api = inject(AdminOrdersService);
  private readonly notify = inject(NotificationService);

  /** Pestaña inicial tomada de la URL (?estado=PENDING). */
  readonly estado = input<string | undefined>();

  readonly tabs = TABS;
  readonly statusLabel = ORDER_STATUS_LABEL;
  readonly activeTab = signal('ALL');
  readonly search = signal('');
  readonly rows = signal<AdminOrderSummary[]>([]);
  readonly stats = signal<OrderStats | null>(null);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly loading = signal(true);

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / PAGE_SIZE)));

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    const initial = this.estado();
    if (initial && TABS.some((tab) => tab.key === initial)) {
      this.activeTab.set(initial);
    }
    this.api.stats().subscribe({ next: (stats) => this.stats.set(stats), error: () => undefined });
    this.load();
  }

  countOf(tab: Tab): number | null {
    const stats = this.stats();
    if (!stats) {
      return null;
    }
    return tab.status ? stats[tab.status] : Object.values(stats).reduce((sum, n) => sum + n, 0);
  }

  selectTab(key: string): void {
    this.activeTab.set(key);
    this.pageIndex.set(0);
    this.load();
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.search.set(value);
      this.pageIndex.set(0);
      this.load();
    }, 300);
  }

  goTo(page: number): void {
    this.pageIndex.set(page);
    this.load();
  }

  private load(): void {
    const tab = TABS.find((item) => item.key === this.activeTab());
    this.loading.set(true);
    this.api.list(tab?.status ?? null, this.search(), this.pageIndex(), PAGE_SIZE).subscribe({
      next: (page) => {
        this.rows.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}

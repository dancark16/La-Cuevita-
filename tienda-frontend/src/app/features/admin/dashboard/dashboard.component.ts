import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AttributesService } from '../catalog/services/attributes.service';
import { ProductsService } from '../catalog/services/products.service';
import { AuthService } from '../../../core/auth/auth.service';
import { DashboardService } from './dashboard.service';

interface QuickAction {
  label: string;
  icon: string;
  route: string;
}

interface DonutSegment {
  key: string;
  label: string;
  count: number;
  percent: number;
  offset: number;
  color: string;
}

const STATUS_ROWS = [
  { key: 'ACTIVE', label: 'Activos', color: '#22c55e' },
  { key: 'DRAFT', label: 'Borradores', color: '#f59e0b' },
  { key: 'INACTIVE', label: 'Inactivos', color: '#9ca3af' },
];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, CurrencyPipe, DecimalPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly dashboard = inject(DashboardService);
  private readonly products = inject(ProductsService);
  private readonly attributes = inject(AttributesService);

  readonly user = inject(AuthService).user;

  readonly stock = toSignal(this.dashboard.stock().pipe(catchError(() => of(null))), {
    initialValue: undefined,
  });
  readonly catalog = toSignal(this.dashboard.catalog().pipe(catchError(() => of(null))), {
    initialValue: undefined,
  });
  readonly recent = toSignal(
    this.products.search({ categoryId: null, q: '', page: 0, size: 4 }).pipe(
      map((page) => page.content),
      catchError(() => of([]))
    ),
    { initialValue: [] }
  );
  readonly sizeCount = toSignal(
    this.attributes.listSizes().pipe(
      map((list) => list.length),
      catchError(() => of(null))
    ),
    { initialValue: null }
  );
  readonly colorCount = toSignal(
    this.attributes.listColors().pipe(
      map((list) => list.length),
      catchError(() => of(null))
    ),
    { initialValue: null }
  );

  readonly pendingOrders = toSignal(
    this.dashboard.orderStats().pipe(
      map((stats) => stats['PENDING'] ?? 0),
      catchError(() => of(0))
    ),
    { initialValue: 0 }
  );

  readonly failed = computed(() => this.stock() === null || this.catalog() === null);
  readonly hasCosts = computed(() => (this.stock()?.costValue ?? 0) > 0);
  readonly profit = computed(() => {
    const stock = this.stock();
    return stock && stock.costValue > 0 ? stock.retailValue - stock.costValue : null;
  });
  readonly attention = computed(() => {
    const stock = this.stock();
    return stock ? stock.lowStock + stock.outOfStock : null;
  });

  readonly productTotal = computed(() => {
    const byStatus = this.catalog()?.productsByStatus;
    return byStatus ? Object.values(byStatus).reduce((sum, n) => sum + n, 0) : null;
  });
  readonly categoryTotal = computed(() => this.catalog()?.categories.length ?? null);

  readonly donut = computed<DonutSegment[]>(() => {
    const byStatus = this.catalog()?.productsByStatus ?? {};
    const total = this.productTotal() || 0;
    let offset = 25;
    return STATUS_ROWS.map((row) => {
      const count = byStatus[row.key] ?? 0;
      const percent = total > 0 ? (count / total) * 100 : 0;
      const segment = { ...row, count, percent, offset };
      offset -= percent;
      return segment;
    });
  });

  readonly topCategories = computed(() => {
    const list = (this.catalog()?.categories ?? []).slice(0, 5);
    const max = list[0]?.products || 1;
    return list.map((category) => ({ ...category, percent: Math.round((category.products / max) * 100) }));
  });

  readonly actions: QuickAction[] = [
    { label: 'Nuevo producto', icon: 'checkroom', route: '/admin/productos' },
    { label: 'Nueva categoría', icon: 'sell', route: '/admin/categorias' },
    { label: 'Importar Excel', icon: 'upload_file', route: '/admin/importar' },
    { label: 'Tallas y colores', icon: 'palette', route: '/admin/atributos' },
  ];
}

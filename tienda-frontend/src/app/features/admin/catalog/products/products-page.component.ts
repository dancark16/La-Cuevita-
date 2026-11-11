import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Category, Product, ProductRow } from '../catalog.models';
import { CatalogItemsService } from '../services/catalog-items.service';
import { CategoriesService } from '../services/categories.service';
import { ProductsListStateService } from '../services/products-list-state.service';
import { ProductsService } from '../services/products.service';
import { ProductFormData, ProductFormDialogComponent } from './product-form-dialog.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-products-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './products-page.component.html',
  styleUrl: './products-page.component.scss',
})
export class ProductsPageComponent implements OnInit {
  private readonly items = inject(CatalogItemsService);
  private readonly products = inject(ProductsService);
  private readonly categoriesApi = inject(CategoriesService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly listState = inject(ProductsListStateService);
  private readonly saved = this.listState.get();

  readonly categories = signal<Category[]>([]);
  readonly rows = signal<ProductRow[]>([]);
  readonly expanded = signal<string | null>(null);
  readonly total = signal(0);
  readonly pageIndex = signal(this.saved.page);
  readonly loading = signal(true);

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / PAGE_SIZE)));
  readonly from = computed(() => (this.total() === 0 ? 0 : this.pageIndex() * PAGE_SIZE + 1));
  readonly to = computed(() => Math.min(this.total(), (this.pageIndex() + 1) * PAGE_SIZE));

  readonly searchControl = new FormControl(this.saved.q, { nonNullable: true });
  readonly categoryControl = new FormControl<string | null>(this.saved.categoryId);
  readonly statusControl = new FormControl<string | null>(this.saved.status);

  constructor() {
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.resetAndLoad());

    this.categoryControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.resetAndLoad());
    this.statusControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.resetAndLoad());
  }

  ngOnInit(): void {
    this.categoriesApi.list().subscribe({
      next: (categories) =>
        this.categories.set([...categories].sort((a, b) => a.name.localeCompare(b.name, 'es'))),
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
    this.load();
  }

  load(): void {
    this.listState.save({
      q: this.searchControl.value,
      categoryId: this.categoryControl.value,
      status: this.statusControl.value,
      page: this.pageIndex(),
    });
    this.loading.set(true);
    this.items
      .searchProducts({
        categoryId: this.categoryControl.value,
        q: this.searchControl.value,
        status: this.statusControl.value,
        page: this.pageIndex(),
        size: PAGE_SIZE,
      })
      .subscribe({
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

  goTo(page: number): void {
    this.pageIndex.set(page);
    this.load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  clearFilters(): void {
    this.searchControl.setValue('');
    this.categoryControl.setValue(null);
    this.statusControl.setValue(null);
  }

  newProduct(): void {
    this.dialog
      .open<ProductFormDialogComponent, ProductFormData, Product>(ProductFormDialogComponent, {
        width: '520px',
        data: { product: null, categories: this.categories() },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.router.navigate(['/admin/productos', saved.id]);
        }
      });
  }

  toggle(id: string): void {
    this.expanded.update((current) => (current === id ? null : id));
  }

  colorsOf(row: ProductRow): { name: string; hex: string }[] {
    const seen = new Map<string, string>();
    row.variants.forEach((v) => seen.set(v.colorName, v.colorHex));
    return [...seen].map(([name, hex]) => ({ name, hex }));
  }

  sizesOf(row: ProductRow): string[] {
    return [...new Set(row.variants.map((v) => v.sizeLabel))];
  }

  deactivate(item: ProductRow): void {
    confirmAction(this.dialog, {
      title: 'Desactivar producto',
      message: `¿Desactivar "${item.name}"? Dejará de mostrarse en la tienda, pero se conserva su historial.`,
      confirmLabel: 'Desactivar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.products.deactivate(item.productId))
      )
      .subscribe({
        next: () => {
          this.notify.success('Producto desactivado');
          this.load();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  private resetAndLoad(): void {
    this.pageIndex.set(0);
    this.load();
  }
}

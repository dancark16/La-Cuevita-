import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { StoreApiService } from '../../data/store-api.service';
import {
  StoreCategory,
  StoreFacets,
  StoreProductSummary,
  StoreSort,
} from '../../data/store.models';
import { ProductCardSkeletonComponent } from '../../shared/product-card-skeleton/product-card-skeleton.component';
import { ProductCardComponent } from '../../shared/product-card/product-card.component';
import { CatalogFiltersComponent } from '../catalog-filters/catalog-filters.component';
import { CatalogSidebarComponent } from '../catalog-sidebar/catalog-sidebar.component';

const PAGE_SIZE = 12;

interface Crumb {
  label: string;
  categoryId: string | null;
}

@Component({
  selector: 'app-catalog-page',
  standalone: true,
  imports: [
    RouterLink,
    CatalogFiltersComponent,
    CatalogSidebarComponent,
    ProductCardComponent,
    ProductCardSkeletonComponent,
  ],
  templateUrl: './catalog-page.component.html',
  styleUrl: './catalog-page.component.scss',
})
export class CatalogPageComponent {
  private readonly api = inject(StoreApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly params$ = this.route.queryParamMap.pipe(
    map((params) => ({
      categoryId: params.get('categoria'),
      q: params.get('q') ?? '',
      sizeLabel: params.get('talla'),
      colorName: params.get('color'),
      sort: (params.get('orden') === 'NAME' ? 'NAME' : 'NEWEST') as StoreSort,
      page: Math.max(0, Number(params.get('pagina') ?? 0) || 0),
    }))
  );

  readonly query = toSignal(this.params$, { requireSync: true });
  readonly categories = toSignal(this.api.allCategories(), { initialValue: [] as StoreCategory[] });
  readonly facets = toSignal(this.api.facets(), { initialValue: { sizes: [], colors: [] } as StoreFacets });

  readonly products = signal<StoreProductSummary[]>([]);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly filtersOpen = signal(false);
  readonly skeletons = Array.from({ length: 8 });

  readonly breadcrumb = computed<Crumb[]>(() => {
    const selected = this.categories().find((category) => category.id === this.query().categoryId);
    if (!selected) {
      return [];
    }
    const parent = selected.parentId
      ? this.categories().find((category) => category.id === selected.parentId)
      : undefined;
    if (!parent) {
      return [{ label: `Todo ${selected.name}`, categoryId: null }];
    }
    return [
      { label: parent.name, categoryId: parent.id },
      { label: selected.name, categoryId: null },
    ];
  });

  readonly activeFilters = computed(() => {
    const query = this.query();
    return [query.sizeLabel, query.colorName].filter(Boolean).length;
  });

  constructor() {
    this.params$
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.failed.set(false);
        }),
        switchMap((query) =>
          this.api.searchProducts({ ...query, size: PAGE_SIZE }).pipe(
            catchError(() => {
              this.failed.set(true);
              return of(null);
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe((page) => {
        this.loading.set(false);
        this.products.set(page?.content ?? []);
        this.total.set(page?.totalElements ?? 0);
        this.totalPages.set(page?.totalPages ?? 0);
      });
  }

  selectCategory(categoryId: string | null): void {
    this.navigate({ categoria: categoryId, pagina: null });
    this.filtersOpen.set(false);
  }

  selectSize(size: string | null): void {
    this.navigate({ talla: size, pagina: null });
  }

  selectColor(color: string | null): void {
    this.navigate({ color, pagina: null });
  }

  search(term: string): void {
    this.navigate({ q: term.trim() || null, pagina: null });
  }

  changeSort(sort: StoreSort): void {
    this.navigate({ orden: sort === 'NAME' ? 'NAME' : null, pagina: null });
  }

  goToPage(page: number): void {
    this.navigate({ pagina: page === 0 ? null : page });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  clearFilters(): void {
    this.navigate({ categoria: null, q: null, orden: null, talla: null, color: null, pagina: null });
    this.filtersOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  closeFilters(): void {
    this.filtersOpen.set(false);
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  private navigate(queryParams: Params): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }
}

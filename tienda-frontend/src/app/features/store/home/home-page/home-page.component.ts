import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { StoreApiService } from '../../data/store-api.service';
import { StoreProductSummary } from '../../data/store.models';
import { CategoryGroup, CategoryTilesComponent } from '../category-tiles/category-tiles.component';
import { CollectionBannerComponent } from '../collection-banner/collection-banner.component';
import { FeaturedProductsComponent } from '../featured-products/featured-products.component';
import { HeroCarouselComponent } from '../hero-carousel/hero-carousel.component';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    HeroCarouselComponent,
    CategoryTilesComponent,
    FeaturedProductsComponent,
    CollectionBannerComponent,
  ],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss',
})
export class HomePageComponent implements OnInit {
  private readonly api = inject(StoreApiService);

  readonly groups = toSignal(
    this.api.rootCategories().pipe(
      switchMap((roots) =>
        roots.length === 0
          ? of([] as CategoryGroup[])
          : forkJoin(
              roots.map((category) =>
                this.api
                  .searchProducts({ categoryId: category.id, q: '', sort: 'NEWEST', page: 0, size: 1 })
                  .pipe(
                    map((page) => ({ category, count: page.totalElements })),
                    catchError(() => of({ category, count: null }))
                  )
              )
            )
      )
    ),
    { initialValue: [] as CategoryGroup[] }
  );

  readonly featured = signal<StoreProductSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api
      .searchProducts({ categoryId: null, q: '', sort: 'NEWEST', page: 0, size: 12 })
      .subscribe({
        next: (page) => {
          // Primero los que tienen foto y stock, luego los demás con stock, y al final los agotados.
          const rank = (product: StoreProductSummary): number =>
            (product.inStock ? 0 : 2) + (product.imageUrl ? 0 : 1);
          this.featured.set([...page.content].sort((a, b) => rank(a) - rank(b)).slice(0, 4));
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}

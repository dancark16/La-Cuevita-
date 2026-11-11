import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';
import { CartService } from '../../cart/cart.service';
import { StoreApiService } from '../../data/store-api.service';
import { ColorSwatch, StoreProductDetail, StoreVariant } from '../../data/store.models';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './product-detail-page.component.html',
  styleUrl: './product-detail-page.component.scss',
})
export class ProductDetailPageComponent {
  private readonly api = inject(StoreApiService);
  private readonly cart = inject(CartService);

  /** Id del producto, tomado de la ruta /producto/:id. */
  readonly id = input.required<string>();

  readonly product = signal<StoreProductDetail | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);

  readonly selectedColor = signal<string | null>(null);
  readonly selectedSize = signal<string | null>(null);
  readonly quantity = signal(1);
  readonly imageIndex = signal(0);
  readonly added = signal(false);
  readonly attempted = signal(false);

  readonly colors = computed<ColorSwatch[]>(() => {
    const seen = new Map<string, ColorSwatch>();
    for (const variant of this.product()?.variants ?? []) {
      if (!seen.has(variant.colorName)) {
        seen.set(variant.colorName, { name: variant.colorName, hex: variant.colorHex });
      }
    }
    return [...seen.values()];
  });

  /** Una de las variantes por color (para saber si el color tiene stock). */
  private readonly variantsOfColor = computed(() =>
    (this.product()?.variants ?? []).filter((variant) => variant.colorName === this.selectedColor())
  );

  readonly sizes = computed(() =>
    this.variantsOfColor().map((variant) => ({
      label: variant.sizeLabel,
      available: variant.maxQuantity > 0,
    }))
  );

  readonly selectedVariant = computed<StoreVariant | null>(
    () => this.variantsOfColor().find((variant) => variant.sizeLabel === this.selectedSize()) ?? null
  );

  readonly images = computed(() => {
    const variants = this.product()?.variants ?? [];
    const current = this.selectedVariant()?.images ?? [];
    if (current.length > 0) {
      return current;
    }
    const ofColor = this.variantsOfColor().flatMap((variant) => variant.images);
    if (ofColor.length > 0) {
      return ofColor;
    }
    return variants.flatMap((variant) => variant.images);
  });

  readonly currentImage = computed(() => this.images()[this.imageIndex()] ?? null);

  readonly price = computed(() => {
    const variant = this.selectedVariant();
    if (variant) {
      return { value: variant.price, compare: variant.compareAtPrice, from: false };
    }
    const prices = (this.product()?.variants ?? []).map((v) => v.price);
    const min = prices.length ? Math.min(...prices) : 0;
    return { value: min, compare: null as number | null, from: new Set(prices).size > 1 };
  });

  readonly inStock = computed(() => (this.product()?.variants ?? []).some((v) => v.maxQuantity > 0));

  readonly stockMessage = computed(() => {
    const variant = this.selectedVariant();
    if (!this.inStock()) {
      return 'Agotado';
    }
    if (!variant) {
      return null;
    }
    if (variant.maxQuantity <= 0) {
      return 'Agotado en esta talla';
    }
    return variant.maxQuantity <= 3
      ? `¡Últimas ${variant.maxQuantity} ${variant.maxQuantity === 1 ? 'unidad' : 'unidades'}!`
      : 'Disponible';
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.notFound.set(false);
        }),
        switchMap((id) => this.api.getProduct(id).pipe(catchError(() => of(null))))
      )
      .subscribe((product) => {
        this.loading.set(false);
        this.product.set(product);
        this.notFound.set(product === null);
        if (product) {
          this.setInitialSelection(product);
        }
      });
  }

  selectColor(name: string): void {
    this.selectedColor.set(name);
    this.imageIndex.set(0);
    const sizes = this.sizes();
    const current = sizes.find((size) => size.label === this.selectedSize());
    if (!current || !current.available) {
      const available = sizes.filter((size) => size.available);
      this.selectedSize.set(available.length === 1 ? available[0].label : null);
    }
    this.quantity.set(1);
  }

  selectSize(label: string, available: boolean): void {
    if (!available) {
      return;
    }
    this.selectedSize.set(label);
    this.imageIndex.set(0);
    this.quantity.set(1);
  }

  changeQuantity(delta: number): void {
    const max = this.selectedVariant()?.maxQuantity ?? 1;
    this.quantity.update((value) => Math.min(Math.max(1, value + delta), max));
  }

  addToCart(): void {
    const product = this.product();
    const variant = this.selectedVariant();
    this.attempted.set(true);
    if (!product || !variant || variant.maxQuantity <= 0) {
      return;
    }
    this.cart.add(
      {
        variantId: variant.id,
        productId: product.id,
        productName: product.name,
        sizeLabel: variant.sizeLabel,
        colorName: variant.colorName,
        colorHex: variant.colorHex,
        unitPrice: variant.price,
        imageUrl: variant.images[0] ?? this.images()[0] ?? null,
        maxQuantity: variant.maxQuantity,
      },
      this.quantity()
    );
    this.added.set(true);
    setTimeout(() => this.added.set(false), 2500);
  }

  private setInitialSelection(product: StoreProductDetail): void {
    const withStock = product.variants.find((variant) => variant.maxQuantity > 0) ?? product.variants[0];
    this.selectedColor.set(withStock?.colorName ?? null);
    this.imageIndex.set(0);
    this.quantity.set(1);
    this.added.set(false);
    this.attempted.set(false);

    const sizes = product.variants.filter(
      (variant) => variant.colorName === withStock?.colorName && variant.maxQuantity > 0
    );
    this.selectedSize.set(sizes.length === 1 ? sizes[0].sizeLabel : null);
  }
}

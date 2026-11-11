import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { catchError, concatMap, filter, forkJoin, from, of, switchMap, toArray } from 'rxjs';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Category, Color, Product, ProductStatus, Size, Variant } from '../catalog.models';
import { AttributesService } from '../services/attributes.service';
import { CatalogItemsService } from '../services/catalog-items.service';
import { CategoriesService } from '../services/categories.service';
import { InventoryService } from '../services/inventory.service';
import { ProductPhotosService } from '../services/product-photos.service';
import { ProductsService } from '../services/products.service';
import { VariantsService } from '../services/variants.service';
import { VariantFormData, VariantFormDialogComponent } from './variant-form-dialog.component';
import { VariantImagesData, VariantImagesDialogComponent } from './variant-images-dialog.component';

interface Photo {
  url: string;
  sku: string;
  /** Variantes que tienen esta misma foto (una foto del producto se guarda en cada variante). */
  refs: { variantId: string; imageId: string }[];
}

const MAX_PHOTOS = 10;

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './product-detail-page.component.html',
  styleUrl: './product-detail-page.component.scss',
})
export class ProductDetailPageComponent implements OnInit {
  @Input({ required: true }) id!: string;

  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly productsApi = inject(ProductsService);
  private readonly variantsApi = inject(VariantsService);
  private readonly attributesApi = inject(AttributesService);
  private readonly categoriesApi = inject(CategoriesService);
  private readonly inventoryApi = inject(InventoryService);
  private readonly catalogItems = inject(CatalogItemsService);
  private readonly photosApi = inject(ProductPhotosService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly maxPhotos = MAX_PHOTOS;

  readonly product = signal<Product | null>(null);
  readonly variants = signal<Variant[]>([]);
  readonly stocks = signal<Record<string, number | undefined>>({});
  readonly categories = signal<Category[]>([]);
  readonly sizes = signal<Size[]>([]);
  readonly colors = signal<Color[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly mixedPrices = signal(false);
  readonly uploading = signal(false);
  readonly dragging = signal(false);

  readonly photos = computed<Photo[]>(() => {
    const byUrl = new Map<string, Photo>();
    for (const variant of this.variants()) {
      for (const image of variant.images) {
        const photo = byUrl.get(image.url) ?? { url: image.url, sku: variant.sku, refs: [] };
        photo.refs.push({ variantId: variant.id, imageId: image.id });
        byUrl.set(image.url, photo);
      }
    }
    return [...byUrl.values()];
  });

  readonly form = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    categoryId: this.fb.nonNullable.control('', Validators.required),
    description: this.fb.nonNullable.control(''),
    price: this.fb.control<number | null>(null, Validators.min(0.01)),
    promoPrice: this.fb.control<number | null>(null, Validators.min(0)),
    active: this.fb.nonNullable.control(true),
  });

  ngOnInit(): void {
    forkJoin({
      product: this.productsApi.get(this.id),
      variants: this.variantsApi.listByProduct(this.id),
      categories: this.categoriesApi.list(),
      sizes: this.attributesApi.listSizes(),
      colors: this.attributesApi.listColors(),
    }).subscribe({
      next: ({ product, variants, categories, sizes, colors }) => {
        this.product.set(product);
        this.variants.set(variants);
        this.categories.set([...categories].sort((a, b) => a.name.localeCompare(b.name, 'es')));
        this.sizes.set(sizes.filter((size) => size.active));
        this.colors.set(colors.filter((color) => color.active));

        const prices = new Set(variants.map((variant) => `${variant.price}|${variant.promoPrice ?? ''}`));
        const common = prices.size === 1 ? variants[0] : null;
        this.mixedPrices.set(prices.size > 1);

        this.form.setValue({
          name: product.name,
          categoryId: product.categoryId,
          description: product.description ?? '',
          price: common ? common.price : null,
          promoPrice: common ? common.promoPrice : null,
          active: product.baseStatus === 'ACTIVE',
        });
        this.loadStocks(variants);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.loadError.set(httpErrorMessage(error, 'No se pudo cargar el producto.'));
      },
    });
  }

  stockOf(variantId: string): number | null {
    return this.stocks()[variantId] ?? null;
  }

  /** Valores escritos en la tabla que aún no se guardan, por variante. */
  readonly stockDrafts = signal<Record<string, number | null>>({});
  readonly stockSaving = signal<string | null>(null);

  stockDraft(variantId: string): number {
    const draft = this.stockDrafts()[variantId];
    return draft === undefined ? (this.stockOf(variantId) ?? 0) : (draft ?? 0);
  }

  stockChanged(variantId: string): boolean {
    const current = this.stockOf(variantId);
    return current !== null && this.stockDraft(variantId) !== current;
  }

  setStockDraft(variantId: string, raw: string): void {
    const value = raw === '' ? null : Math.max(0, Math.floor(Number(raw)));
    this.stockDrafts.update((drafts) => ({ ...drafts, [variantId]: Number.isNaN(value) ? null : value }));
  }

  saveStock(variant: Variant): void {
    const current = this.stockOf(variant.id) ?? 0;
    const target = this.stockDrafts()[variant.id];
    if (target === null || target === undefined || target === current) {
      return;
    }
    this.stockSaving.set(variant.id);
    this.inventoryApi.adjust(variant.id, target - current, 'Ajuste manual desde el panel').subscribe({
      next: () => {
        this.stocks.update((stocks) => ({ ...stocks, [variant.id]: target }));
        this.stockDrafts.update(({ [variant.id]: _removed, ...rest }) => rest);
        this.stockSaving.set(null);
        this.notify.success(`Stock actualizado: ${variant.colorName} ${variant.sizeLabel} → ${target}`);
      },
      error: (error) => {
        this.stockSaving.set(null);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }

  invalid(name: 'name' | 'categoryId' | 'price' | 'promoPrice'): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  save(): void {
    const product = this.product();
    if (!product) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const price = value.price === null || (value.price as unknown) === '' ? null : Number(value.price);
    const promo =
      value.promoPrice === null || (value.promoPrice as unknown) === '' ? 0 : Number(value.promoPrice);
    if (price !== null && promo > 0 && promo >= price) {
      this.notify.error('El precio promocional debe ser menor al precio normal.');
      return;
    }

    const baseStatus: ProductStatus = value.active
      ? 'ACTIVE'
      : product.baseStatus === 'DRAFT'
        ? 'DRAFT'
        : 'INACTIVE';

    this.saving.set(true);
    this.productsApi
      .update(this.id, {
        categoryId: value.categoryId,
        name: value.name.trim(),
        description: value.description.trim() || null,
        baseStatus,
      })
      .pipe(
        switchMap((saved) => {
          this.product.set(saved);
          if (price === null || this.variants().length === 0) {
            return of(null);
          }
          return forkJoin(
            this.variants().map((variant) =>
              this.catalogItems.update(variant.id, { price, promoPrice: promo })
            )
          );
        })
      )
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.notify.success('Cambios guardados');
          this.reloadVariants();
        },
        error: (error) => {
          this.saving.set(false);
          this.notify.error(httpErrorMessage(error));
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/admin/productos']);
  }

  reloadVariants(): void {
    this.variantsApi.listByProduct(this.id).subscribe({
      next: (variants) => {
        this.variants.set(variants);
        this.loadStocks(variants);
      },
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
  }

  onFilesChosen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    this.uploadPhotos(files);
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
    this.uploadPhotos(Array.from(event.dataTransfer?.files ?? []));
  }

  removePhoto(photo: Photo): void {
    confirmAction(this.dialog, {
      title: 'Quitar foto',
      message: '¿Quitar esta foto del producto?',
      confirmLabel: 'Quitar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() =>
          from(photo.refs).pipe(
            concatMap((ref) => this.variantsApi.removeImage(ref.variantId, ref.imageId)),
            toArray()
          )
        )
      )
      .subscribe({
        next: () => {
          this.notify.success('Foto quitada');
          this.reloadVariants();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  private uploadPhotos(files: File[]): void {
    const variants = this.variants();
    if (files.length === 0) {
      return;
    }
    if (variants.length === 0) {
      this.notify.error('Primero agrega una variante para poder subir fotos.');
      return;
    }
    if (this.photos().length + files.length > this.maxPhotos) {
      this.notify.error(`Un producto puede tener hasta ${this.maxPhotos} fotos.`);
      return;
    }

    this.uploading.set(true);
    this.photosApi
      .attach(
        variants.map((variant) => ({ variantId: variant.id, currentCount: variant.images.length })),
        files
      )
      .subscribe({
        next: (count) => {
          this.uploading.set(false);
          this.notify.success(count === 1 ? 'Foto subida' : `${count} fotos subidas`);
          this.reloadVariants();
        },
        error: (error) => {
          this.uploading.set(false);
          this.notify.error(error instanceof Error ? error.message : httpErrorMessage(error));
        },
      });
  }

  openVariantForm(variant: Variant | null): void {
    this.dialog
      .open<VariantFormDialogComponent, VariantFormData, boolean>(VariantFormDialogComponent, {
        width: '560px',
        data: { productId: this.id, variant, sizes: this.sizes(), colors: this.colors(), existing: this.variants() },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.reloadVariants();
        }
      });
  }

  openImages(variant: Variant): void {
    this.dialog
      .open<VariantImagesDialogComponent, VariantImagesData, boolean>(VariantImagesDialogComponent, {
        width: '600px',
        data: { variant },
      })
      .afterClosed()
      .subscribe((changed) => {
        if (changed) {
          this.reloadVariants();
        }
      });
  }

  discontinue(variant: Variant): void {
    confirmAction(this.dialog, {
      title: 'Eliminar variante',
      message: `¿Eliminar ${variant.colorName} ${variant.sizeLabel} (${variant.sku})? Si ya tiene pedidos se oculta y se conserva su historial; si no, se borra por completo.`,
      confirmLabel: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.variantsApi.discontinue(variant.id))
      )
      .subscribe({
        next: () => {
          this.notify.success('Variante eliminada');
          this.reloadVariants();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  private loadStocks(variants: Variant[]): void {
    if (variants.length === 0) {
      this.stocks.set({});
      return;
    }
    forkJoin(
      variants.map((variant) =>
        this.inventoryApi.get(variant.id).pipe(catchError(() => of({ quantityOnHand: 0 })))
      )
    ).subscribe((results) => {
      const map: Record<string, number> = {};
      variants.forEach((variant, index) => (map[variant.id] = results[index].quantityOnHand));
      this.stocks.set(map);
    });
  }
}

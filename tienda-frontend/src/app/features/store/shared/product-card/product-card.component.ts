import { CurrencyPipe } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../cart/cart.service';
import { StoreProductSummary } from '../../data/store.models';

interface SizeChip {
  label: string;
  available: boolean;
}

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
})
export class ProductCardComponent implements OnInit {
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);

  @Input({ required: true }) product!: StoreProductSummary;

  sizes: SizeChip[] = [];
  readonly selectedSize = signal<string | null>(null);
  readonly added = signal(false);

  get lastUnits(): boolean {
    const total = this.product.variants.reduce((sum, variant) => sum + variant.maxQuantity, 0);
    return this.product.inStock && total > 0 && total <= 2;
  }

  ngOnInit(): void {
    const availability = new Map<string, boolean>();
    for (const variant of this.product.variants) {
      const already = availability.get(variant.sizeLabel) ?? false;
      availability.set(variant.sizeLabel, already || variant.maxQuantity > 0);
    }
    this.sizes = [...availability.entries()].map(([label, available]) => ({ label, available }));
  }

  selectSize(chip: SizeChip): void {
    if (!chip.available) {
      return;
    }
    this.selectedSize.update((current) => (current === chip.label ? null : chip.label));
  }

  addToCart(): void {
    const singleColor = this.product.colors.length === 1;
    const onlySize = this.sizes.length === 1 && this.sizes[0].available ? this.sizes[0].label : null;
    const label = this.selectedSize() ?? onlySize;
    const variant = label
      ? this.product.variants.find((v) => v.sizeLabel === label && v.maxQuantity > 0)
      : undefined;

    if (!singleColor || !variant) {
      this.router.navigate(['/producto', this.product.id]);
      return;
    }

    this.cart.add(
      {
        variantId: variant.id,
        productId: this.product.id,
        productName: this.product.name,
        sizeLabel: variant.sizeLabel,
        colorName: variant.colorName,
        colorHex: variant.colorHex,
        unitPrice: variant.price,
        imageUrl: this.product.imageUrl,
        maxQuantity: variant.maxQuantity,
      },
      1
    );

    this.added.set(true);
    setTimeout(() => this.added.set(false), 1500);
  }
}

import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreProductSummary } from '../../data/store.models';
import { ProductCardSkeletonComponent } from '../../shared/product-card-skeleton/product-card-skeleton.component';
import { ProductCardComponent } from '../../shared/product-card/product-card.component';

@Component({
  selector: 'app-featured-products',
  standalone: true,
  imports: [RouterLink, ProductCardComponent, ProductCardSkeletonComponent],
  templateUrl: './featured-products.component.html',
  styleUrl: './featured-products.component.scss',
})
export class FeaturedProductsComponent {
  @Input({ required: true }) products!: StoreProductSummary[];
  @Input() loading = false;

  readonly skeletons = Array.from({ length: 4 });
}

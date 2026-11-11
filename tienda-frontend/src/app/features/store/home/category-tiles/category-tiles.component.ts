import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreCategory } from '../../data/store.models';

/** Fotos de las categorías principales, usadas cuando la categoría no tiene una imagen propia en el panel. */
const DEFAULT_IMAGES: Record<string, string> = {
  mujer: 'assets/images/categorias/mujer.png',
  hombre: 'assets/images/categorias/hombre.webp',
  accesorios: 'assets/images/categorias/accesorios.webp',
};

export interface CategoryGroup {
  category: StoreCategory;
  count: number | null;
}

@Component({
  selector: 'app-category-tiles',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './category-tiles.component.html',
  styleUrl: './category-tiles.component.scss',
})
export class CategoryTilesComponent {
  /** Categorías principales (Mujer, Hombre, Accesorios) con su número de productos. */
  @Input({ required: true }) groups!: CategoryGroup[];
  imageFor(category: StoreCategory): string | null {
    return category.imageUrl ?? DEFAULT_IMAGES[category.slug] ?? null;
  }
}

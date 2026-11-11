import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { StoreCategory, StoreFacets } from '../../data/store.models';

const SIZE_ORDER = ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL'];
const GENERIC_COLOR = 'único';

const ICONS: Record<string, string> = {
  mujer: 'checkroom',
  hombre: 'dry_cleaning',
  accesorios: 'shopping_bag',
};

@Component({
  selector: 'app-catalog-sidebar',
  standalone: true,
  templateUrl: './catalog-sidebar.component.html',
  styleUrl: './catalog-sidebar.component.scss',
})
export class CatalogSidebarComponent {
  @Input({ required: true }) categories!: StoreCategory[];
  @Input() selectedCategoryId: string | null = null;
  @Input() facets: StoreFacets = { sizes: [], colors: [] };
  @Input() selectedSize: string | null = null;
  @Input() selectedColor: string | null = null;

  @Output() categorySelected = new EventEmitter<string | null>();
  @Output() sizeSelected = new EventEmitter<string | null>();
  @Output() colorSelected = new EventEmitter<string | null>();
  @Output() cleared = new EventEmitter<void>();

  private readonly toggled = signal<Record<string, boolean>>({});

  get roots(): StoreCategory[] {
    return this.categories.filter((category) => category.parentId === null);
  }

  get sizes(): string[] {
    const rank = (label: string): number => {
      const letter = SIZE_ORDER.indexOf(label.toUpperCase());
      if (letter >= 0) {
        return letter;
      }
      const numeric = Number(label);
      return Number.isNaN(numeric) ? 100 : 20 + numeric;
    };
    return [...this.facets.sizes].sort((a, b) => rank(a) - rank(b) || a.localeCompare(b, 'es'));
  }

  get colors() {
    return this.facets.colors.filter((color) => color.name.toLowerCase() !== GENERIC_COLOR);
  }

  get hasFilters(): boolean {
    return !!(this.selectedCategoryId || this.selectedSize || this.selectedColor);
  }

  childrenOf(root: StoreCategory): StoreCategory[] {
    return this.categories.filter((category) => category.parentId === root.id);
  }

  icon(root: StoreCategory): string {
    return ICONS[root.slug] ?? 'sell';
  }

  activeRootId(): string | null {
    const selected = this.categories.find((category) => category.id === this.selectedCategoryId);
    return selected ? (selected.parentId ?? selected.id) : null;
  }

  isOpen(root: StoreCategory): boolean {
    const manual = this.toggled()[root.id];
    return manual ?? this.activeRootId() === root.id;
  }

  toggle(root: StoreCategory): void {
    this.toggled.update((state) => ({ ...state, [root.id]: !this.isOpen(root) }));
  }
}

import { Injectable, computed, effect, signal } from '@angular/core';

export interface CartLine {
  variantId: string;
  productId: string;
  productName: string;
  sizeLabel: string;
  colorName: string;
  colorHex: string;
  unitPrice: number;
  imageUrl: string | null;
  quantity: number;
  maxQuantity: number;
}

const STORAGE_KEY = 'lc_cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _lines = signal<CartLine[]>(this.restore());

  readonly lines = this._lines.asReadonly();
  readonly count = computed(() => this._lines().reduce((total, line) => total + line.quantity, 0));
  readonly subtotal = computed(() =>
    this._lines().reduce((total, line) => total + line.unitPrice * line.quantity, 0)
  );

  constructor() {
    effect(() => {
      const lines = this._lines();
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
      } catch {
        /* almacenamiento no disponible: el carrito sigue en memoria */
      }
    });
  }

  add(line: Omit<CartLine, 'quantity'>, quantity: number): void {
    this._lines.update((lines) => {
      const existing = lines.find((item) => item.variantId === line.variantId);
      if (!existing) {
        return [...lines, { ...line, quantity: Math.min(quantity, line.maxQuantity) }];
      }
      return lines.map((item) =>
        item.variantId === line.variantId
          ? {
              ...item,
              unitPrice: line.unitPrice,
              maxQuantity: line.maxQuantity,
              quantity: Math.min(item.quantity + quantity, line.maxQuantity),
            }
          : item
      );
    });
  }

  setQuantity(variantId: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(variantId);
      return;
    }
    this._lines.update((lines) =>
      lines.map((item) =>
        item.variantId === variantId
          ? { ...item, quantity: Math.min(quantity, item.maxQuantity) }
          : item
      )
    );
  }

  remove(variantId: string): void {
    this._lines.update((lines) => lines.filter((item) => item.variantId !== variantId));
  }

  clear(): void {
    this._lines.set([]);
  }

  private restore(): CartLine[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as CartLine[]) : [];
    } catch {
      return [];
    }
  }
}

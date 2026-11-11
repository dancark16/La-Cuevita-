import { Injectable } from '@angular/core';

export interface ProductsListState {
  q: string;
  categoryId: string | null;
  status: string | null;
  page: number;
}

const KEY = 'admin.products.list';

/** Recuerda la búsqueda, los filtros y la página del listado para volver exactamente donde estabas. */
@Injectable({ providedIn: 'root' })
export class ProductsListStateService {
  private state: ProductsListState = this.read();

  get(): ProductsListState {
    return { ...this.state };
  }

  save(state: ProductsListState): void {
    this.state = { ...state };
    try {
      sessionStorage.setItem(KEY, JSON.stringify(this.state));
    } catch {
      // sin almacenamiento: queda solo en memoria
    }
  }

  private read(): ProductsListState {
    try {
      const raw = sessionStorage.getItem(KEY);
      if (raw) {
        return { q: '', categoryId: null, status: null, page: 0, ...JSON.parse(raw) };
      }
    } catch {
      // se ignora
    }
    return { q: '', categoryId: null, status: null, page: 0 };
  }
}

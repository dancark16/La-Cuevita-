import { Injectable } from '@angular/core';

export interface ProductsListState {
  q: string;
  categoryId: string | null;
  status: string | null;
  stock: string | null;
  page: number;
}

const KEY = 'admin.products.list';

/** Recuerda la búsqueda, los filtros y la página del listado para volver exactamente donde estabas. */
@Injectable({ providedIn: 'root' })
export class ProductsListStateService {
  private state: ProductsListState = this.read();

  /** true si el usuario ya había usado el listado antes (hay estado guardado). */
  hasSaved(): boolean {
    try {
      return sessionStorage.getItem(KEY) !== null;
    } catch {
      return false;
    }
  }

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
        return { q: '', categoryId: null, status: null, stock: null, page: 0, ...JSON.parse(raw) };
      }
    } catch {
      // se ignora
    }
    return { q: '', categoryId: null, status: null, stock: null, page: 0 };
  }
}

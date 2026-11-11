import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Page } from '../../../../shared/models/page.model';
import { CatalogItem, CatalogItemUpdate, ProductRow } from '../catalog.models';

export interface CatalogItemSearch {
  categoryId: string | null;
  q: string;
  status: string | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogItemsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/catalog/items`;

  search(search: CatalogItemSearch): Observable<Page<CatalogItem>> {
    let params = new HttpParams().set('page', search.page).set('size', search.size);
    if (search.categoryId) {
      params = params.set('categoryId', search.categoryId);
    }
    if (search.status) {
      params = params.set('status', search.status);
    }
    if (search.q.trim()) {
      params = params.set('q', search.q.trim());
    }
    return this.http.get<Page<CatalogItem>>(this.url, { params });
  }

  searchProducts(search: CatalogItemSearch): Observable<Page<ProductRow>> {
    let params = new HttpParams().set('page', search.page).set('size', search.size);
    if (search.categoryId) {
      params = params.set('categoryId', search.categoryId);
    }
    if (search.status) {
      params = params.set('status', search.status);
    }
    if (search.q.trim()) {
      params = params.set('q', search.q.trim());
    }
    return this.http.get<Page<ProductRow>>(`${this.url}/products`, { params });
  }

  update(variantId: string, request: CatalogItemUpdate): Observable<CatalogItem> {
    return this.http.patch<CatalogItem>(`${this.url}/${variantId}`, request);
  }
}

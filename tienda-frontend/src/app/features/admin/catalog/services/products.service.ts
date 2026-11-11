import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Page } from '../../../../shared/models/page.model';
import { Product, ProductRequest } from '../catalog.models';

export interface ProductSearch {
  categoryId: string | null;
  q: string;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/products`;

  search(search: ProductSearch): Observable<Page<Product>> {
    let params = new HttpParams()
      .set('page', search.page)
      .set('size', search.size)
      .set('sort', 'createdAt,desc');

    if (search.categoryId) {
      params = params.set('categoryId', search.categoryId);
    }
    if (search.q.trim()) {
      params = params.set('q', search.q.trim());
    }
    return this.http.get<Page<Product>>(this.url, { params });
  }

  get(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.url}/${id}`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.url, request);
  }

  update(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.url}/${id}`, request);
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

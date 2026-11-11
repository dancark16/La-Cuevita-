import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { ImageRequest, Variant, VariantRequest } from '../catalog.models';

@Injectable({ providedIn: 'root' })
export class VariantsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/variants`;

  listByProduct(productId: string): Observable<Variant[]> {
    const params = new HttpParams().set('productId', productId);
    return this.http.get<Variant[]>(this.url, { params });
  }

  create(request: VariantRequest): Observable<Variant> {
    return this.http.post<Variant>(this.url, request);
  }

  update(id: string, request: VariantRequest): Observable<Variant> {
    return this.http.put<Variant>(`${this.url}/${id}`, request);
  }

  discontinue(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  addImage(id: string, request: ImageRequest): Observable<Variant> {
    return this.http.post<Variant>(`${this.url}/${id}/images`, request);
  }

  removeImage(id: string, imageId: string): Observable<Variant> {
    return this.http.delete<Variant>(`${this.url}/${id}/images/${imageId}`);
  }
}

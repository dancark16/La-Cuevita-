import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';
import { CheckoutPayload, Order } from '../../../shared/models/order.model';

export const MAX_RECEIPT_BYTES = 10 * 1024 * 1024;
export const RECEIPT_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/orders`;

  checkout(payload: CheckoutPayload): Observable<Order> {
    return this.http.post<Order>(this.url, payload);
  }

  get(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.url}/${id}`);
  }

  mine(page = 0, size = 20): Observable<Page<Order>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<Order>>(this.url, { params });
  }

  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/cancel`, null);
  }

  uploadReceipt(id: string, file: File): Observable<Order> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<Order>(`${this.url}/${id}/receipt`, body);
  }

  /** Motivo si el archivo no sirve como comprobante, o null si está bien. */
  validateReceipt(file: File): string | null {
    if (!RECEIPT_TYPES.includes(file.type)) {
      return 'Sube una imagen (JPG, PNG, WebP) o un PDF.';
    }
    if (file.size > MAX_RECEIPT_BYTES) {
      return 'El archivo supera los 10 MB.';
    }
    return null;
  }
}

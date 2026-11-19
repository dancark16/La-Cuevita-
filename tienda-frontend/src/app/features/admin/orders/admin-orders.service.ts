import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Order } from '../../../shared/models/order.model';
import { Page } from '../../../shared/models/page.model';
import { OrderStatus } from '../../../shared/util/order-status';

export interface AdminOrderSummary {
  id: string;
  orderNumber: number;
  status: OrderStatus;
  customerName: string;
  customerPhone: string | null;
  total: number;
  itemCount: number;
  hasReceipt: boolean;
  receiptUploadedAt: string | null;
  createdAt: string;
}

export type OrderStats = Record<OrderStatus, number>;

export interface ShipPayload {
  carrierId: string | null;
  trackingNumber: string | null;
  note: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminOrdersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/orders`;

  list(status: string | null, q: string, page: number, size = 20): Observable<Page<AdminOrderSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<Page<AdminOrderSummary>>(this.url, { params });
  }

  stats(): Observable<OrderStats> {
    return this.http.get<OrderStats>(`${this.url}/stats`);
  }

  get(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.url}/${id}`);
  }

  receipt(id: string): Observable<Blob> {
    return this.http.get(`${this.url}/${id}/receipt`, { responseType: 'blob' });
  }

  confirmPayment(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/confirm-payment`, null);
  }

  rejectReceipt(id: string, note: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/reject-receipt`, { note });
  }

  ship(id: string, request: ShipPayload): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/ship`, request);
  }

  addTrackingEvent(id: string, message: string, notifyCustomer: boolean): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/tracking-events`, { message, notifyCustomer });
  }

  deleteTrackingEvent(id: string, eventId: string): Observable<Order> {
    return this.http.delete<Order>(`${this.url}/${id}/tracking-events/${eventId}`);
  }

  deliver(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/deliver`, null);
  }

  cancel(id: string, note: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/cancel`, { note });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

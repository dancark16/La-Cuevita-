import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface Carrier {
  id: string;
  name: string;
  trackingUrl: string | null;
  active: boolean;
}

export interface CarrierRequest {
  name: string;
  trackingUrl: string | null;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class CarriersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/carriers`;

  list(): Observable<Carrier[]> {
    return this.http.get<Carrier[]>(this.url);
  }

  create(request: CarrierRequest): Observable<Carrier> {
    return this.http.post<Carrier>(this.url, request);
  }

  update(id: string, request: CarrierRequest): Observable<Carrier> {
    return this.http.put<Carrier>(`${this.url}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

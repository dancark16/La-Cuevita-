import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface InventoryInfo {
  variantId: string;
  sku: string;
  quantityOnHand: number;
  quantityReserved: number;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);

  get(variantId: string): Observable<InventoryInfo> {
    return this.http.get<InventoryInfo>(`${environment.apiUrl}/inventory/${variantId}`);
  }

  /** Deja el stock en una cantidad exacta registrando un ajuste (queda en el historial de movimientos). */
  adjust(variantId: string, delta: number, reason: string): Observable<unknown> {
    return this.http.post(`${environment.apiUrl}/inventory/movements`, {
      variantId,
      type: 'AJUSTE',
      quantity: delta,
      reason,
    });
  }
}

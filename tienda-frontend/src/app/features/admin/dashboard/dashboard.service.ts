import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface StockSummary {
  units: number;
  retailValue: number;
  costValue: number;
  variants: number;
  outOfStock: number;
  lowStock: number;
}

export interface CategoryCount {
  id: string;
  name: string;
  products: number;
}

export interface CatalogStats {
  productsByStatus: Record<string, number>;
  categories: CategoryCount[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/dashboard`;

  stock(): Observable<StockSummary> {
    return this.http.get<StockSummary>(`${this.url}/stock`);
  }

  orderStats(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${environment.apiUrl}/admin/orders/stats`);
  }

  catalog(): Observable<CatalogStats> {
    return this.http.get<CatalogStats>(`${this.url}/catalog`);
  }
}

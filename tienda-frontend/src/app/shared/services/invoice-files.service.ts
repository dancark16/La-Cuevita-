import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Descarga de la factura en PDF y reenvío por correo. */
@Injectable({ providedIn: 'root' })
export class InvoiceFilesService {
  private readonly http = inject(HttpClient);

  /** Baja el PDF de la factura de un pedido. */
  download(orderId: string, orderNumber: number): Observable<Blob> {
    return this.http
      .get(`${environment.apiUrl}/orders/${orderId}/invoice/pdf`, { responseType: 'blob' })
      .pipe(
        tap((blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `Factura-pedido-${orderNumber}.pdf`;
          link.click();
          setTimeout(() => URL.revokeObjectURL(url), 5000);
        })
      );
  }

  /** Solo administración: reenvía la factura al correo de la clienta. */
  sendByEmail(orderId: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/admin/orders/${orderId}/invoice/email`, null);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PublicSettings {
  whatsappNumber: string | null;
  instagramUser: string | null;
  payphoneEnabled: boolean;
  bankNames: string[];
}

export interface BankAccount {
  id: string;
  bankName: string;
  accountType: string;
  accountNumber: string;
  holder: string;
  holderId: string | null;
  holderEmail: string | null;
  swiftCode: string | null;
  active: boolean;
}

const DEFAULTS: PublicSettings = {
  whatsappNumber: null,
  instagramUser: null,
  payphoneEnabled: false,
  bankNames: [],
};

/** Configuración de la tienda (WhatsApp, Instagram, bancos...) que administra el dueño desde el panel. */
@Injectable({ providedIn: 'root' })
export class StoreSettingsService {
  private readonly http = inject(HttpClient);

  private readonly _settings = signal<PublicSettings>(DEFAULTS);
  readonly settings = this._settings.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    this.http
      .get<PublicSettings>(`${environment.apiUrl}/store/settings`)
      .pipe(catchError(() => of(DEFAULTS)))
      .subscribe((settings) => this._settings.set(settings));
  }

  /** Cuentas para transferir; solo responde con sesión iniciada. */
  bankAccounts(): Observable<BankAccount[]> {
    return this.http.get<{ bankAccounts: BankAccount[] }>(`${environment.apiUrl}/payment-info`).pipe(
      map((info) => info.bankAccounts),
      catchError(() => of([] as BankAccount[]))
    );
  }
}

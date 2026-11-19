import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { BankAccount } from '../../../core/settings/store-settings.service';

export interface AdminSettings {
  whatsappNumber: string | null;
  instagramUser: string | null;
  payphoneEnabled: boolean;
  bankAccounts: BankAccount[];
  contactEmail: string | null;
}

export interface SettingsRequest {
  whatsappNumber: string | null;
  instagramUser: string | null;
  payphoneEnabled: boolean;
  contactEmail: string | null;
}

export interface BillingProfile {
  businessName: string | null;
  tradeName: string | null;
  ruc: string | null;
  headOfficeAddress: string | null;
  branchAddress: string | null;
  specialTaxpayer: string | null;
  accountingRequired: boolean;
}

export interface BankAccountRequest {
  bankName: string;
  accountType: string;
  accountNumber: string;
  holder: string;
  holderId: string | null;
  holderEmail: string | null;
  swiftCode: string | null;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminSettingsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/settings`;

  getBilling(): Observable<BillingProfile> {
    return this.http.get<BillingProfile>(`${this.url}/billing`);
  }

  updateBilling(request: BillingProfile): Observable<BillingProfile> {
    return this.http.put<BillingProfile>(`${this.url}/billing`, request);
  }

  get(): Observable<AdminSettings> {
    return this.http.get<AdminSettings>(this.url);
  }

  update(request: SettingsRequest): Observable<AdminSettings> {
    return this.http.put<AdminSettings>(this.url, request);
  }

  createBank(request: BankAccountRequest): Observable<BankAccount> {
    return this.http.post<BankAccount>(`${this.url}/bank-accounts`, request);
  }

  updateBank(id: string, request: BankAccountRequest): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.url}/bank-accounts/${id}`, request);
  }

  deleteBank(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/bank-accounts/${id}`);
  }
}

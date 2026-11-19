import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs';
import { BankAccount, StoreSettingsService } from '../../../../core/settings/store-settings.service';
import { bankLogo } from '../../../../shared/util/bank-logo';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { AdminSettings, AdminSettingsService, BillingProfile } from '../admin-settings.service';
import { CarrierModalComponent } from '../carrier-modal/carrier-modal.component';
import { Carrier, CarriersService } from '../carriers.service';
import { BankAccountModalComponent } from '../bank-account-modal/bank-account-modal.component';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [ReactiveFormsModule, BankAccountModalComponent, CarrierModalComponent],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss',
})
export class SettingsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminSettingsService);
  private readonly publicSettings = inject(StoreSettingsService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly logoOf = bankLogo;
  private readonly carriersApi = inject(CarriersService);
  readonly carriers = signal<Carrier[]>([]);
  readonly carrierModalOpen = signal(false);
  readonly editingCarrier = signal<Carrier | null>(null);

  openCarrier(carrier: Carrier | null): void {
    this.editingCarrier.set(carrier);
    this.carrierModalOpen.set(true);
  }

  onCarrierSaved(): void {
    this.carrierModalOpen.set(false);
    this.loadCarriers();
  }

  removeCarrier(carrier: Carrier): void {
    confirmAction(this.dialog, {
      title: 'Eliminar transportista',
      message: `¿Eliminar a ${carrier.name}? Los pedidos que ya enviaste con él conservan su nombre y su guía.`,
      confirmLabel: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.carriersApi.delete(carrier.id))
      )
      .subscribe({
        next: () => {
          this.notify.success('Transportista eliminado');
          this.loadCarriers();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  private loadCarriers(): void {
    this.carriersApi.list().subscribe({ next: (list) => this.carriers.set(list), error: () => undefined });
  }

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly accounts = signal<BankAccount[]>([]);
  readonly payphoneEnabled = signal(false);
  readonly modalOpen = signal(false);
  readonly editing = signal<BankAccount | null>(null);

  readonly savingBilling = signal(false);

  readonly billingForm = this.fb.nonNullable.group({
    businessName: [''],
    tradeName: [''],
    ruc: [''],
    headOfficeAddress: [''],
    branchAddress: [''],
    specialTaxpayer: [''],
    accountingRequired: [false],
  });

  saveBilling(): void {
    const v = this.billingForm.getRawValue();
    const ruc = v.ruc.trim();
    if (ruc && !/^\d{13}$/.test(ruc)) {
      this.notify.error('El RUC debe tener 13 dígitos.');
      return;
    }
    const request: BillingProfile = {
      businessName: v.businessName.trim() || null,
      tradeName: v.tradeName.trim() || null,
      ruc: ruc || null,
      headOfficeAddress: v.headOfficeAddress.trim() || null,
      branchAddress: v.branchAddress.trim() || null,
      specialTaxpayer: v.specialTaxpayer.trim() || null,
      accountingRequired: v.accountingRequired,
    };
    this.savingBilling.set(true);
    this.api.updateBilling(request).subscribe({
      next: () => {
        this.savingBilling.set(false);
        this.notify.success('Datos de facturación guardados');
      },
      error: (error) => {
        this.savingBilling.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }

  readonly form = this.fb.nonNullable.group({
    whatsappNumber: [''],
    instagramUser: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  save(): void {
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.api
      .update({
        whatsappNumber: value.whatsappNumber.trim() || null,
        instagramUser: value.instagramUser.trim() || null,
        payphoneEnabled: this.payphoneEnabled(),
      })
      .subscribe({
        next: (settings) => {
          this.apply(settings);
          this.saving.set(false);
          this.publicSettings.reload();
          this.notify.success('Cambios guardados');
        },
        error: (error) => {
          this.saving.set(false);
          this.notify.error(httpErrorMessage(error));
        },
      });
  }

  openModal(account: BankAccount | null): void {
    this.editing.set(account);
    this.modalOpen.set(true);
  }

  onSaved(): void {
    this.modalOpen.set(false);
    this.load();
    this.publicSettings.reload();
  }

  remove(account: BankAccount): void {
    confirmAction(this.dialog, {
      title: 'Eliminar cuenta',
      message: `¿Eliminar la cuenta de ${account.bankName} terminada en ${account.accountNumber.slice(-4)}? Las clientas dejarán de verla.`,
      confirmLabel: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteBank(account.id))
      )
      .subscribe({
        next: () => {
          this.notify.success('Cuenta eliminada');
          this.load();
          this.publicSettings.reload();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  private load(): void {
    this.loadCarriers();
    this.api.getBilling().subscribe({
      next: (billing) =>
        this.billingForm.setValue({
          businessName: billing.businessName ?? '',
          tradeName: billing.tradeName ?? '',
          ruc: billing.ruc ?? '',
          headOfficeAddress: billing.headOfficeAddress ?? '',
          branchAddress: billing.branchAddress ?? '',
          specialTaxpayer: billing.specialTaxpayer ?? '',
          accountingRequired: billing.accountingRequired,
        }),
      error: () => undefined,
    });
    this.api.get().subscribe({
      next: (settings) => {
        this.apply(settings);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }

  private apply(settings: AdminSettings): void {
    this.form.setValue({
      whatsappNumber: settings.whatsappNumber ?? '',
      instagramUser: settings.instagramUser ?? '',
    });
    this.payphoneEnabled.set(settings.payphoneEnabled);
    this.accounts.set(settings.bankAccounts);
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs';
import { BankAccount, StoreSettingsService } from '../../../../core/settings/store-settings.service';
import { bankLogo } from '../../../../shared/util/bank-logo';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { AdminSettings, AdminSettingsService } from '../admin-settings.service';
import { BankAccountModalComponent } from '../bank-account-modal/bank-account-modal.component';

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [ReactiveFormsModule, BankAccountModalComponent],
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
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly accounts = signal<BankAccount[]>([]);
  readonly payphoneEnabled = signal(false);
  readonly modalOpen = signal(false);
  readonly editing = signal<BankAccount | null>(null);

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

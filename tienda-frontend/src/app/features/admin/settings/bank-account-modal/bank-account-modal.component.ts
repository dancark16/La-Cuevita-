import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BankAccount } from '../../../../core/settings/store-settings.service';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { AdminSettingsService } from '../admin-settings.service';

const BANKS = [
  'Banco de Guayaquil',
  'Banco Pichincha',
  'Banco del Pacífico',
  'Produbanco',
  'Banco Bolivariano',
  'Banco Internacional',
  'Banco del Austro',
  'Cooperativa JEP',
  'Cooperativa Jardín Azuayo',
  'Otro',
];

@Component({
  selector: 'app-bank-account-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './bank-account-modal.component.html',
  styleUrl: './bank-account-modal.component.scss',
})
export class BankAccountModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminSettingsService);
  private readonly notify = inject(NotificationService);

  readonly account = input<BankAccount | null>(null);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly banks = BANKS;
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    bankName: ['', [Validators.required, Validators.maxLength(80)]],
    accountType: ['Ahorros', Validators.required],
    accountNumber: ['', [Validators.required, Validators.pattern(/^[0-9-]{6,30}$/)]],
    holder: ['', [Validators.required, Validators.maxLength(120)]],
    holderId: ['', Validators.maxLength(20)],
    holderEmail: ['', [Validators.maxLength(150), Validators.email]],
    swiftCode: ['', Validators.maxLength(20)],
    active: [true],
  });

  ngOnInit(): void {
    const current = this.account();
    if (current) {
      this.form.setValue({
        bankName: current.bankName,
        accountType: current.accountType,
        accountNumber: current.accountNumber,
        holder: current.holder,
        holderId: current.holderId ?? '',
        holderEmail: current.holderEmail ?? '',
        swiftCode: current.swiftCode ?? '',
        active: current.active,
      });
    }
  }

  invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request = {
      bankName: value.bankName.trim(),
      accountType: value.accountType,
      accountNumber: value.accountNumber.trim(),
      holder: value.holder.trim(),
      holderId: value.holderId.trim() || null,
      holderEmail: value.holderEmail.trim() || null,
      swiftCode: value.swiftCode.trim() || null,
      active: value.active,
    };
    const current = this.account();
    const call$ = current ? this.api.updateBank(current.id, request) : this.api.createBank(request);

    this.saving.set(true);
    call$.subscribe({
      next: () => {
        this.notify.success(current ? 'Cuenta actualizada' : 'Cuenta agregada');
        this.saved.emit();
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}

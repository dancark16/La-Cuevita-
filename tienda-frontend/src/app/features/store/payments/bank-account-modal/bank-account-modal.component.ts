import { CurrencyPipe } from '@angular/common';
import { Component, input, output, signal } from '@angular/core';
import { BankAccount } from '../../../../core/settings/store-settings.service';
import { bankLogo } from '../../../../shared/util/bank-logo';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';

/** Datos de una cuenta bancaria para que la clienta haga su transferencia. */
@Component({
  selector: 'app-bank-account-modal',
  standalone: true,
  imports: [ModalComponent, CurrencyPipe],
  templateUrl: './bank-account-modal.component.html',
  styleUrl: './bank-account-modal.component.scss',
})
export class BankAccountModalComponent {
  readonly account = input.required<BankAccount>();
  /** Monto a transferir, si se conoce (por ejemplo, el total del carrito). */
  readonly amount = input<number | null>(null);
  readonly closed = output<void>();

  readonly logoOf = bankLogo;
  readonly copied = signal<string | null>(null);

  copy(key: string, value: string): void {
    navigator.clipboard?.writeText(value).then(() => {
      this.copied.set(key);
      setTimeout(() => this.copied.set(null), 1500);
    });
  }
}

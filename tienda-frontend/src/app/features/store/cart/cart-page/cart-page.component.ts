import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { bankLogo } from '../../../../shared/util/bank-logo';
import { BankAccount, StoreSettingsService } from '../../../../core/settings/store-settings.service';
import { BankAccountModalComponent } from '../../payments/bank-account-modal/bank-account-modal.component';
import { CartService } from '../cart.service';

@Component({
    selector: 'app-cart-page',
    standalone: true,
    imports: [RouterLink, CurrencyPipe, BankAccountModalComponent],
    templateUrl: './cart-page.component.html',
    styleUrl: './cart-page.component.scss',
})
export class CartPageComponent {
    readonly cart = inject(CartService);
    private readonly settingsService = inject(StoreSettingsService);
    private readonly settings = this.settingsService.settings;

    readonly logoOf = bankLogo;
    readonly accounts = signal<BankAccount[]>([]);
    readonly selectedAccount = signal<BankAccount | null>(null);
    readonly payphoneEnabled = computed(() => this.settings().payphoneEnabled);

    constructor() {
        this.settingsService.bankAccounts().subscribe((accounts) => this.accounts.set(accounts));
    }

    readonly hasWhatsapp = computed(() => !!this.settings().whatsappNumber);

    readonly whatsappUrl = computed(() => {
        const lines = this.cart.lines().map(
            (line) =>
                `• ${line.quantity} x ${line.productName} (Talla ${line.sizeLabel}, ${line.colorName}) - $${(
                    line.unitPrice * line.quantity
                ).toFixed(2)}`
        );
        const message = [
            'Hola La Cuevita, quiero hacer este pedido:',
            '',
            ...lines,
            '',
            `Total: $${this.cart.subtotal().toFixed(2)}`,
        ].join('\n');
        return `https://wa.me/${this.settings().whatsappNumber}?text=${encodeURIComponent(message)}`;
    });

    increase(variantId: string, quantity: number): void {
        this.cart.setQuantity(variantId, quantity + 1);
    }

    decrease(variantId: string, quantity: number): void {
        this.cart.setQuantity(variantId, quantity - 1);
    }
}
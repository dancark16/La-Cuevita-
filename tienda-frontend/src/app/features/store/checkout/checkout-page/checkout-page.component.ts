import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { StoreSettingsService } from '../../../../core/settings/store-settings.service';
import { CartService } from '../../cart/cart.service';
import { OrderApiService } from '../../orders/order-api.service';
import { CheckoutPayload, IdentificationType } from '../../../../shared/models/order.model';

const PROVINCES = [
  'Azuay', 'Bolívar', 'Cañar', 'Carchi', 'Chimborazo', 'Cotopaxi', 'El Oro', 'Esmeraldas',
  'Galápagos', 'Guayas', 'Imbabura', 'Loja', 'Los Ríos', 'Manabí', 'Morona Santiago', 'Napo',
  'Orellana', 'Pastaza', 'Pichincha', 'Santa Elena', 'Santo Domingo de los Tsáchilas',
  'Sucumbíos', 'Tungurahua', 'Zamora Chinchipe',
];

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss',
})
export class CheckoutPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly orders = inject(OrderApiService);
  readonly cart = inject(CartService);

  readonly provinces = PROVINCES;
  private readonly settings = inject(StoreSettingsService).settings;
  readonly bankNames = computed(() => this.settings().bankNames);
  readonly payphoneEnabled = computed(() => this.settings().payphoneEnabled);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    idType: ['CEDULA' as IdentificationType, Validators.required],
    idNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20)]],
    firstName: ['', Validators.required],
    lastName: [''],
    phone: ['', [Validators.required, Validators.pattern(/^[+\d][\d\s-]{7,16}$/)]],
    line1: ['', Validators.required],
    city: ['', Validators.required],
    province: ['', Validators.required],
    reference: [''],
  });

  invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  submit(): void {
    if (this.cart.lines().length === 0) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const isCompany = value.idType === 'RUC';
    const payload: CheckoutPayload = {
      customerProfile: {
        idType: value.idType,
        idNumber: value.idNumber.trim(),
        firstName: isCompany ? null : value.firstName.trim(),
        lastName: isCompany ? null : value.lastName.trim() || null,
        businessName: isCompany ? value.firstName.trim() : null,
        phone: value.phone.trim(),
      },
      address: {
        line1: value.line1.trim(),
        line2: null,
        city: value.city.trim(),
        province: value.province,
        reference: value.reference.trim() || null,
      },
      items: this.cart.lines().map((line) => ({ variantId: line.variantId, quantity: line.quantity })),
    };

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.orders.checkout(payload).subscribe({
      next: (order) => {
        this.cart.clear();
        this.router.navigate(['/pedido', order.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(this.messageFor(error));
      },
    });
  }

  private messageFor(error: HttpErrorResponse): string {
    const message = (error.error as { message?: string } | null)?.message;
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    if (error.status === 409 || error.status === 400) {
      return message ?? 'Revisa los datos e intenta de nuevo.';
    }
    return 'No pudimos crear tu pedido. Intenta de nuevo.';
  }
}

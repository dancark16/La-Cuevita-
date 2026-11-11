import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { concatMap, from, toArray } from 'rxjs';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Color, Size, Variant, VariantRequest, VariantStatus } from '../catalog.models';
import { VariantsService } from '../services/variants.service';

export interface VariantFormData {
  productId: string;
  variant: Variant | null;
  sizes: Size[];
  colors: Color[];
  /** Variantes que ya tiene el producto, para no repetir combinaciones. */
  existing: Variant[];
}

const STATUS_OPTIONS: { value: VariantStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Activa' },
  { value: 'INACTIVE', label: 'Inactiva' },
  { value: 'DISCONTINUED', label: 'Descontinuada' },
];

function promoBelowPrice(control: AbstractControl): ValidationErrors | null {
  const promo = control.value as number | null;
  const price = control.parent?.get('price')?.value as number | null | undefined;
  return promo !== null && price !== null && price !== undefined && promo >= price
    ? { promoTooHigh: true }
    : null;
}

function code(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-zA-Z0-9]/g, '')
    .toUpperCase()
    .slice(0, 3);
}

@Component({
  selector: 'app-variant-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  templateUrl: './variant-form-dialog.component.html',
  styleUrl: './variant-form-dialog.component.scss',
})
export class VariantFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(VariantsService);
  private readonly notify = inject(NotificationService);
  private readonly dialogRef = inject(MatDialogRef<VariantFormDialogComponent, boolean>);
  readonly data = inject<VariantFormData>(MAT_DIALOG_DATA);

  readonly editing = this.data.variant !== null;
  readonly saving = signal(false);
  readonly statusOptions = STATUS_OPTIONS;

  readonly selectedColors = signal<string[]>(this.data.variant ? [this.data.variant.colorId] : []);
  readonly selectedSizes = signal<string[]>(this.data.variant ? [this.data.variant.sizeId] : []);
  readonly attempted = signal(false);

  /** Combinaciones talla+color elegidas; las que ya existen se marcan y no se crean. */
  readonly combos = computed(() => {
    const taken = new Set(
      this.data.existing.filter((v) => v.id !== this.data.variant?.id).map((v) => `${v.sizeId}|${v.colorId}`)
    );
    const result: { size: Size; color: Color; duplicate: boolean }[] = [];
    for (const color of this.data.colors.filter((c) => this.selectedColors().includes(c.id))) {
      for (const size of this.data.sizes.filter((s) => this.selectedSizes().includes(s.id))) {
        result.push({ size, color, duplicate: taken.has(`${size.id}|${color.id}`) });
      }
    }
    return result;
  });

  readonly newCombos = computed(() => this.combos().filter((c) => !c.duplicate));

  private readonly firstExisting = this.data.existing[0];

  readonly form = this.fb.group({
    sku: this.fb.nonNullable.control(this.data.variant?.sku ?? this.suggestSku(), Validators.required),
    barcode: this.fb.nonNullable.control(this.data.variant?.barcode ?? ''),
    price: this.fb.control<number | null>(this.data.variant?.price ?? this.firstExisting?.price ?? null, [
      Validators.required,
      Validators.min(0),
    ]),
    promoPrice: this.fb.control<number | null>(
      this.data.variant?.promoPrice ?? this.firstExisting?.promoPrice ?? null,
      [Validators.min(0), promoBelowPrice]
    ),
    status: this.fb.nonNullable.control<VariantStatus>(this.data.variant?.status ?? 'ACTIVE'),
  });

  constructor() {
    this.form.controls.price.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.promoPrice.updateValueAndValidity());
  }

  get missingAttributes(): boolean {
    return this.data.sizes.length === 0 || this.data.colors.length === 0;
  }

  toggleColor(id: string): void {
    this.selectedColors.update((list) => this.toggle(list, id));
  }

  toggleSize(id: string): void {
    this.selectedSizes.update((list) => this.toggle(list, id));
  }

  skuFor(combo: { size: Size; color: Color }): string {
    return `${this.form.controls.sku.value.trim()}-${code(combo.size.label)}-${code(combo.color.name)}`;
  }

  save(): void {
    this.attempted.set(true);
    if (this.form.invalid || this.selectedColors().length === 0 || this.selectedSizes().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const build = (sizeId: string, colorId: string, sku: string): VariantRequest => ({
      productId: this.data.productId,
      sizeId,
      colorId,
      sku,
      barcode: value.barcode.trim() || null,
      price: value.price as number,
      promoPrice: value.promoPrice,
      status: value.status,
    });

    const pending = this.newCombos();
    if (pending.length === 0) {
      this.notify.error('Esas combinaciones ya existen en este producto.');
      return;
    }

    const original = this.data.variant;
    // Al editar, la variante actual se conserva (o pasa a la primera combinación si la quitaron)
    // y el resto de combinaciones elegidas se crean como variantes nuevas.
    const target = original
      ? (pending.find((c) => c.size.id === original.sizeId && c.color.id === original.colorId) ?? pending[0])
      : null;
    const extras = pending.filter((c) => c !== target);

    const calls = [
      ...(original && target
        ? [this.api.update(original.id, build(target.size.id, target.color.id, value.sku.trim()))]
        : []),
      ...extras.map((combo) => this.api.create(build(combo.size.id, combo.color.id, this.skuFor(combo)))),
    ];

    this.saving.set(true);
    from(calls)
      .pipe(concatMap((call) => call), toArray())
      .subscribe({
        next: () =>
          this.done(
            extras.length === 0
              ? original ? 'Variante actualizada' : 'Variante creada'
              : original
                ? `Variante actualizada y ${extras.length} nueva${extras.length === 1 ? '' : 's'} creada${extras.length === 1 ? '' : 's'}`
                : `${extras.length} variantes creadas`
          ),
        error: (error) => this.fail(error),
      });
  }

  private done(message: string): void {
    this.notify.success(message);
    this.dialogRef.close(true);
  }

  private fail(error: unknown): void {
    this.saving.set(false);
    this.notify.error(httpErrorMessage(error));
  }

  private toggle(list: string[], id: string): string[] {
    return list.includes(id) ? list.filter((item) => item !== id) : [...list, id];
  }

  private suggestSku(): string {
    const sku = this.data.existing[0]?.sku ?? '';
    return sku.replace(/-[^-]+-[^-]+$/, '');
  }
}

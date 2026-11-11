import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Category, Product, ProductRequest, ProductStatus } from '../catalog.models';
import { ProductsService } from '../services/products.service';

export interface ProductFormData {
  product: Product | null;
  categories: Category[];
}

const STATUS_OPTIONS: { value: ProductStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'DRAFT', label: 'Borrador' },
  { value: 'INACTIVE', label: 'Inactivo' },
];

@Component({
  selector: 'app-product-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.product ? 'Editar producto' : 'Nuevo producto' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content class="!flex flex-col gap-1">
        @if (data.categories.length === 0) {
          <p class="mb-2 rounded bg-amber-100 p-3 text-sm text-amber-900">
            Primero crea al menos una categoría.
          </p>
        }

        <mat-form-field appearance="outline">
          <mat-label>Categoría</mat-label>
          <mat-select formControlName="categoryId">
            @for (category of data.categories; track category.id) {
              <mat-option [value]="category.id">{{ category.name }}</mat-option>
            }
          </mat-select>
          @if (form.controls.categoryId.hasError('required')) {
            <mat-error>Selecciona una categoría</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.hasError('required')) {
            <mat-error>El nombre es obligatorio</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Descripción</mat-label>
          <textarea matInput rows="3" formControlName="description"></textarea>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Estado</mat-label>
          <mat-select formControlName="baseStatus">
            @for (option of statusOptions; track option.value) {
              <mat-option [value]="option.value">{{ option.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button
          mat-flat-button
          color="primary"
          type="submit"
          [disabled]="saving() || data.categories.length === 0"
        >
          Guardar
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ProductFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductsService);
  private readonly notify = inject(NotificationService);
  private readonly dialogRef = inject(MatDialogRef<ProductFormDialogComponent, Product>);
  readonly data = inject<ProductFormData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly statusOptions = STATUS_OPTIONS;

  readonly form = this.fb.group({
    categoryId: this.fb.control<string | null>(this.data.product?.categoryId ?? null, Validators.required),
    name: this.fb.nonNullable.control(this.data.product?.name ?? '', Validators.required),
    description: this.fb.nonNullable.control(this.data.product?.description ?? ''),
    baseStatus: this.fb.nonNullable.control<ProductStatus>(this.data.product?.baseStatus ?? 'ACTIVE'),
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: ProductRequest = {
      categoryId: value.categoryId as string,
      name: value.name.trim(),
      description: value.description.trim() || null,
      baseStatus: value.baseStatus,
    };

    const call$ = this.data.product
      ? this.api.update(this.data.product.id, request)
      : this.api.create(request);

    this.saving.set(true);
    call$.subscribe({
      next: (product) => {
        this.notify.success(this.data.product ? 'Producto actualizado' : 'Producto creado');
        this.dialogRef.close(product);
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}

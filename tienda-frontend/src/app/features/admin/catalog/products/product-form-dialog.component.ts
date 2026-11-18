import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
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
  imports: [ReactiveFormsModule, MatDialogModule, ModalComponent, SelectComponent],
  templateUrl: './product-form-dialog.component.html',
  styleUrl: './product-form-dialog.component.scss',
})
export class ProductFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductsService);
  private readonly notify = inject(NotificationService);
  private readonly dialogRef = inject(MatDialogRef<ProductFormDialogComponent, Product>);
  readonly data = inject<ProductFormData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly statusOptions = STATUS_OPTIONS;
  readonly statusChoices: SelectOption[] = STATUS_OPTIONS.map((o) => ({ value: o.value, label: o.label }));
  readonly categoryOptions: SelectOption[] = this.data.categories.map((c) => ({ value: c.id, label: c.name }));

  readonly form = this.fb.group({
    categoryId: this.fb.control<string | null>(this.data.product?.categoryId ?? null, Validators.required),
    name: this.fb.nonNullable.control(this.data.product?.name ?? '', Validators.required),
    description: this.fb.nonNullable.control(this.data.product?.description ?? ''),
    baseStatus: this.fb.nonNullable.control<ProductStatus>(this.data.product?.baseStatus ?? 'ACTIVE'),
  });

  cancel(): void {
    this.dialogRef.close();
  }

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

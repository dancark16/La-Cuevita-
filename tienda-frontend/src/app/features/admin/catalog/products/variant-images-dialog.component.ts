import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Variant, VariantImage } from '../catalog.models';
import { ProductPhotosService } from '../services/product-photos.service';
import { VariantsService } from '../services/variants.service';

export interface VariantImagesData {
  variant: Variant;
}

@Component({
  selector: 'app-variant-images-dialog',
  standalone: true,
  templateUrl: './variant-images-dialog.component.html',
  styleUrl: './variant-images-dialog.component.scss',
})
export class VariantImagesDialogComponent {
  private readonly photos = inject(ProductPhotosService);
  private readonly api = inject(VariantsService);
  private readonly notify = inject(NotificationService);
  private readonly dialogRef = inject(MatDialogRef<VariantImagesDialogComponent, boolean>);
  readonly data = inject<VariantImagesData>(MAT_DIALOG_DATA);

  readonly images = signal<VariantImage[]>(this.data.variant.images);
  readonly changed = signal(false);
  readonly uploading = signal(false);
  readonly dragging = signal(false);

  onChosen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    this.upload(files);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.upload(Array.from(event.dataTransfer?.files ?? []));
  }

  remove(image: VariantImage): void {
    this.api.removeImage(this.data.variant.id, image.id).subscribe({
      next: (variant) => {
        this.images.set(variant.images);
        this.changed.set(true);
      },
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
  }

  close(): void {
    this.dialogRef.close(this.changed());
  }

  private upload(files: File[]): void {
    if (files.length === 0) {
      return;
    }
    this.uploading.set(true);
    this.photos
      .attach([{ variantId: this.data.variant.id, currentCount: this.images().length }], files)
      .subscribe({
        next: (count) => {
          this.notify.success(count === 1 ? 'Foto subida' : `${count} fotos subidas`);
          this.refresh();
        },
        error: (error) => {
          this.uploading.set(false);
          this.notify.error(error instanceof Error ? error.message : httpErrorMessage(error));
        },
      });
  }

  private refresh(): void {
    this.api.listByProduct(this.data.variant.productId).subscribe({
      next: (variants) => {
        const current = variants.find((variant) => variant.id === this.data.variant.id);
        this.images.set(current?.images ?? []);
        this.changed.set(true);
        this.uploading.set(false);
      },
      error: (error) => {
        this.uploading.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}

import { Injectable, inject } from '@angular/core';
import { Observable, concatMap, from, map, of, switchMap, throwError, toArray } from 'rxjs';
import { Variant } from '../catalog.models';
import { FilesService } from './files.service';
import { VariantsService } from './variants.service';

/** Sube fotos desde el computador y las asocia a una o varias variantes. */
@Injectable({ providedIn: 'root' })
export class ProductPhotosService {
  private readonly files = inject(FilesService);
  private readonly variants = inject(VariantsService);

  /**
   * Sube las imágenes y las agrega al final de las fotos de cada variante indicada.
   * `current` trae la cantidad de fotos actuales de cada variante para continuar el orden.
   */
  attach(targets: { variantId: string; currentCount: number }[], images: File[]): Observable<number> {
    const invalid = images.map((file) => this.files.validateImage(file)).find((reason) => reason !== null);
    if (invalid) {
      return throwError(() => new Error(invalid));
    }
    if (images.length === 0 || targets.length === 0) {
      return of(0);
    }

    return this.files.uploadProductImages(images).pipe(
      switchMap((uploaded) => {
        const tasks = targets.flatMap((target) =>
          uploaded.map((file, index) => ({
            variantId: target.variantId,
            url: file.url,
            sortOrder: target.currentCount + index,
          }))
        );
        return from(tasks).pipe(
          concatMap((task) =>
            this.variants.addImage(task.variantId, { url: task.url, sortOrder: task.sortOrder })
          ),
          toArray(),
          map(() => uploaded.length)
        );
      })
    );
  }

  /** Atajo para una sola variante. */
  attachToVariant(variant: Variant, images: File[]): Observable<number> {
    return this.attach([{ variantId: variant.id, currentCount: variant.images.length }], images);
  }
}

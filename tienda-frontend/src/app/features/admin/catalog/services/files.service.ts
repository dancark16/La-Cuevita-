import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface UploadedFile {
  id: string;
  url: string;
  originalName: string | null;
  sizeBytes: number;
}

export const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

@Injectable({ providedIn: 'root' })
export class FilesService {
  private readonly http = inject(HttpClient);

  uploadProductImages(files: File[]): Observable<UploadedFile[]> {
    const body = new FormData();
    files.forEach((file) => body.append('files', file, file.name));
    return this.http.post<UploadedFile[]>(`${environment.apiUrl}/admin/files/product-images`, body);
  }

  /** Devuelve el motivo si el archivo no sirve como foto de producto, o null si está bien. */
  validateImage(file: File): string | null {
    if (!IMAGE_TYPES.includes(file.type)) {
      return `"${file.name}" no es JPG, PNG ni WebP.`;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      return `"${file.name}" supera los 10 MB.`;
    }
    return null;
  }
}

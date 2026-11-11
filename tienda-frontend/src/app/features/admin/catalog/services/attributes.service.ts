import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Color, ColorRequest, Size, SizeRequest } from '../catalog.models';

@Injectable({ providedIn: 'root' })
export class AttributesService {
  private readonly http = inject(HttpClient);
  private readonly sizesUrl = `${environment.apiUrl}/sizes`;
  private readonly colorsUrl = `${environment.apiUrl}/colors`;

  listSizes(): Observable<Size[]> {
    return this.http.get<Size[]>(this.sizesUrl);
  }

  createSize(request: SizeRequest): Observable<Size> {
    return this.http.post<Size>(this.sizesUrl, request);
  }

  updateSize(id: string, request: SizeRequest): Observable<Size> {
    return this.http.put<Size>(`${this.sizesUrl}/${id}`, request);
  }

  deleteSize(id: string): Observable<void> {
    return this.http.delete<void>(`${this.sizesUrl}/${id}`);
  }

  listColors(): Observable<Color[]> {
    return this.http.get<Color[]>(this.colorsUrl);
  }

  createColor(request: ColorRequest): Observable<Color> {
    return this.http.post<Color>(this.colorsUrl, request);
  }

  updateColor(id: string, request: ColorRequest): Observable<Color> {
    return this.http.put<Color>(`${this.colorsUrl}/${id}`, request);
  }

  deleteColor(id: string): Observable<void> {
    return this.http.delete<void>(`${this.colorsUrl}/${id}`);
  }
}

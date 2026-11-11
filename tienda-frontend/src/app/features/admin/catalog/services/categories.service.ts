import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Category, CategoryRequest } from '../catalog.models';

@Injectable({ providedIn: 'root' })
export class CategoriesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/categories`;

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.url);
  }

  create(request: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.url, request);
  }

  update(id: string, request: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.url}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

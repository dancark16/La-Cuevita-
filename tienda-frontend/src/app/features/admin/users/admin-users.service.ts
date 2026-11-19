import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';

export type UserRoleName = 'ADMIN' | 'CUSTOMER';

export interface AdminUser {
  id: string;
  email: string;
  role: UserRoleName;
  enabled: boolean;
  createdAt: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role: UserRoleName;
}

@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/users`;

  search(q: string, role: string | null, page: number, size: number): Observable<Page<AdminUser>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (role) {
      params = params.set('role', role);
    }
    return this.http.get<Page<AdminUser>>(this.url, { params });
  }

  create(request: CreateUserRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>(this.url, request);
  }

  update(id: string, changes: { role?: UserRoleName; enabled?: boolean }): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.url}/${id}`, changes);
  }

  sendResetLink(id: string): Observable<void> {
    return this.http.post<void>(`${this.url}/${id}/reset-link`, null);
  }
}

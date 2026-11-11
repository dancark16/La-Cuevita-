import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ImportResult } from './import.models';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/imports/products`;

  preview(file: File): Observable<ImportResult> {
    return this.send(file, true);
  }

  apply(file: File): Observable<ImportResult> {
    return this.send(file, false);
  }

  private send(file: File, dryRun: boolean): Observable<ImportResult> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<ImportResult>(this.url, body, {
      params: new HttpParams().set('dryRun', dryRun),
    });
  }
}

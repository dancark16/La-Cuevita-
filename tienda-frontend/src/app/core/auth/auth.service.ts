import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, shareReplay, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AccessTokenResponse, LoginRequest, SessionUser, UserRole } from './auth.models';
import { decodeJwt } from './jwt.util';

interface AccessTokenClaims {
  sub: string;
  role: UserRole;
  exp: number;
}

const WITH_COOKIES = { withCredentials: true };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private accessToken: string | null = null;
  private readonly _user = signal<SessionUser | null>(null);
  private refreshInFlight$: Observable<AccessTokenResponse> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  getAccessToken(): string | null {
    return this.accessToken;
  }

  login(request: LoginRequest): Observable<AccessTokenResponse> {
    return this.http
      .post<AccessTokenResponse>(`${environment.apiUrl}/auth/login`, request, WITH_COOKIES)
      .pipe(tap((response) => this.setSession(response)));
  }

  register(request: LoginRequest): Observable<AccessTokenResponse> {
    return this.http
      .post(`${environment.apiUrl}/auth/register`, request)
      .pipe(switchMap(() => this.login(request)));
  }

  loginWithGoogle(idToken: string): Observable<AccessTokenResponse> {
    return this.http
      .post<AccessTokenResponse>(`${environment.apiUrl}/auth/google`, { idToken }, WITH_COOKIES)
      .pipe(tap((response) => this.setSession(response)));
  }

  refresh(): Observable<AccessTokenResponse> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<AccessTokenResponse>(`${environment.apiUrl}/auth/refresh`, null, WITH_COOKIES)
        .pipe(
          tap((response) => this.setSession(response)),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay(1)
        );
    }
    return this.refreshInFlight$;
  }

  restoreSession(): Observable<unknown> {
    return this.refresh().pipe(catchError(() => of(null)));
  }

  logout(): void {
    this.http
      .post(`${environment.apiUrl}/auth/logout`, null, WITH_COOKIES)
      .subscribe({ error: () => undefined });
    this.expireSession();
  }

  expireSession(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  clearSession(): void {
    this.accessToken = null;
    this._user.set(null);
  }

  private setSession(response: AccessTokenResponse): void {
    this.accessToken = response.accessToken;
    const claims = decodeJwt<AccessTokenClaims>(response.accessToken);
    this._user.set(claims ? { email: claims.sub, role: claims.role } : null);
  }
}

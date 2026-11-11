import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const isApiCall = req.url.startsWith(environment.apiUrl);
  const isAuthCall = req.url.startsWith(`${environment.apiUrl}/auth/`);
  if (!isApiCall || isAuthCall) {
    return next(req);
  }

  const withToken = (token: string | null) =>
    token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(withToken(auth.getAccessToken())).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError(() => {
          auth.expireSession();
          return throwError(() => error);
        }),
        switchMap((response) => next(withToken(response.accessToken)))
      );
    })
  );
};

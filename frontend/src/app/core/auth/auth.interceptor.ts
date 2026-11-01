import { HttpBackend, HttpClient, HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { AuthResponse } from './auth.models';
import { AuthStore } from './auth.store';

let refreshing: Observable<string> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthStore);
  const router = inject(Router);
  const raw = new HttpClient(inject(HttpBackend));
  return next(withToken(req, store.accessToken())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isAuthPath(req.url) || req.headers.has('X-Ada-Retry')) {
        return throwError(() => error);
      }
      const refreshToken = store.refreshToken();
      if (!refreshToken) {
        store.clear();
        void router.navigate(['/login']);
        return throwError(() => error);
      }
      refreshing ??= raw.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken }).pipe(
        tap((response) => store.setSession(response)),
        map((response) => response.accessToken),
        catchError((refreshError) => {
          store.clear();
          void router.navigate(['/login']);
          return throwError(() => refreshError);
        }),
        finalize(() => {
          refreshing = null;
        }),
        shareReplay(1),
      );
      return refreshing.pipe(switchMap((token) => next(withToken(req, token, true))));
    }),
  );
};

function withToken(req: HttpRequest<unknown>, token: string | null, retry = false): HttpRequest<unknown> {
  let headers = req.headers;
  if (token && !isAuthPath(req.url)) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }
  if (retry) {
    headers = headers.set('X-Ada-Retry', '1');
  }
  return req.clone({ headers });
}

function isAuthPath(url: string): boolean {
  return url.includes('/api/v1/auth/login') || url.includes('/api/v1/auth/register') || url.includes('/api/v1/auth/refresh');
}

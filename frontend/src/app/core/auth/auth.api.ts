import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';
import { AuthResponse, MeResponse, UserProfile } from './auth.models';
import { AuthStore } from './auth.store';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly store = inject(AuthStore);

  register(body: {
    companyName: string;
    slug: string | null;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/register', body);
  }

  login(body: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', body);
  }

  me(): Observable<MeResponse> {
    return this.http.get<MeResponse>('/api/v1/auth/me');
  }

  refreshSession(): Observable<AuthResponse> {
    const refreshToken = this.store.refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('Refresh token is missing.'));
    }
    return this.http.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken }).pipe(
      tap((response) => this.store.setSession(response)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', { refreshToken: this.store.refreshToken() });
  }

  updateProfile(body: { firstName: string; lastName: string }): Observable<UserProfile> {
    return this.http.patch<UserProfile>('/api/v1/auth/me', body);
  }

  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.post<void>('/api/v1/auth/password', body);
  }
}

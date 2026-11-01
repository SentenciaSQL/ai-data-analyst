import { Injectable, signal } from '@angular/core';
import { AuthResponse, MeResponse, TenantProfile, UserProfile } from './auth.models';

const ACCESS = 'ada.access';
const REFRESH = 'ada.refresh';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly access = signal<string | null>(localStorage.getItem(ACCESS));
  private readonly refresh = signal<string | null>(localStorage.getItem(REFRESH));
  readonly user = signal<UserProfile | null>(null);
  readonly tenant = signal<TenantProfile | null>(null);
  readonly ready = signal(false);

  accessToken(): string | null {
    return this.access();
  }

  refreshToken(): string | null {
    return this.refresh();
  }

  has(permission: string): boolean {
    return this.user()?.permissions.includes(permission) ?? false;
  }

  setSession(response: AuthResponse): void {
    this.access.set(response.accessToken);
    this.refresh.set(response.refreshToken);
    localStorage.setItem(ACCESS, response.accessToken);
    localStorage.setItem(REFRESH, response.refreshToken);
    this.user.set(response.user);
    this.tenant.set(response.tenant);
  }

  setProfile(profile: MeResponse): void {
    this.user.set(profile.user);
    this.tenant.set(profile.tenant);
  }

  setUser(user: UserProfile): void {
    this.user.set(user);
  }

  clear(): void {
    this.access.set(null);
    this.refresh.set(null);
    this.user.set(null);
    this.tenant.set(null);
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
  }

  markReady(): void {
    this.ready.set(true);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Billing, Overview, RoleProfile, TenantProfile, UserProfile } from '../auth/auth.models';

@Injectable({ providedIn: 'root' })
export class WorkspaceApi {
  private readonly http = inject(HttpClient);

  overview(): Observable<Overview> {
    return this.http.get<Overview>('/api/v1/overview');
  }

  tenant(): Observable<TenantProfile> {
    return this.http.get<TenantProfile>('/api/v1/tenant');
  }

  updateTenant(name: string): Observable<TenantProfile> {
    return this.http.patch<TenantProfile>('/api/v1/tenant', { name });
  }

  users(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>('/api/v1/users');
  }

  roles(): Observable<RoleProfile[]> {
    return this.http.get<RoleProfile[]>('/api/v1/roles');
  }

  createUser(body: {
    email: string;
    firstName: string;
    lastName: string;
    password: string;
    roleCode: UserProfile['roleCode'];
  }): Observable<UserProfile> {
    return this.http.post<UserProfile>('/api/v1/users', body);
  }

  updateUser(id: string, body: {
    firstName: string;
    lastName: string;
    roleCode: UserProfile['roleCode'];
    status: UserProfile['status'];
  }): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`/api/v1/users/${id}`, body);
  }

  billing(): Observable<Billing> {
    return this.http.get<Billing>('/api/v1/billing');
  }
}

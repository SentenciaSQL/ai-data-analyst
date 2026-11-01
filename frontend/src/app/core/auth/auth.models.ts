export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  status: 'ACTIVE' | 'DISABLED';
  roleCode: 'OWNER' | 'ADMIN' | 'ANALYST' | 'VIEWER';
  roleName: string;
  permissions: string[];
  lastLoginAt?: string;
  createdAt: string;
}

export interface TenantProfile {
  id: string;
  name: string;
  slug: string;
  status: 'ACTIVE' | 'SUSPENDED';
  planCode: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserProfile;
  tenant: TenantProfile;
}

export interface MeResponse {
  user: UserProfile;
  tenant: TenantProfile;
}

export interface RoleProfile {
  code: UserProfile['roleCode'];
  name: string;
  description: string;
  systemRole: boolean;
  permissions: string[];
}

export interface AuditEvent {
  id: string;
  actorUserId?: string;
  action: string;
  resourceType?: string;
  resourceId?: string;
  metadata?: Record<string, string>;
  createdAt: string;
}

export interface Overview {
  tenant: TenantProfile;
  teamSize: number;
  activeUsers: number;
  aiEngineStatus: string;
  aiProvider?: string;
  aiProviderConfigured: boolean;
  recentActivity: AuditEvent[];
}

export interface Billing {
  planCode: string;
  status: string;
  message: string;
}

export interface ApiErrorBody {
  code?: string;
  message?: string;
  fields?: { field: string; message: string }[];
}

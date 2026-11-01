import { Routes } from '@angular/router';
import { authGuard, guestGuard, permissionGuard } from './core/auth/auth.guard';

const pending = (title: string, body: string) => ({
  title,
  body,
});

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      { path: 'overview', loadComponent: () => import('./features/overview/overview-page').then((m) => m.OverviewPage) },
      {
        path: 'analyst',
        canActivate: [permissionGuard],
        data: { permission: 'conversations:read' },
        loadComponent: () => import('./features/analyst/analyst-page').then((m) => m.AnalystPage),
      },
      {
        path: 'connections',
        canActivate: [permissionGuard],
        data: {
          permission: 'connections:read',
          ...pending(
            'Connections',
            'Connect a read-only account on the database you already run. PostgreSQL is the first engine, then MySQL. Credentials are encrypted, and customer rows stay in the customer database.',
          ),
        },
        loadComponent: () => import('./features/pending/pending-page').then((m) => m.PendingPage),
      },
      {
        path: 'catalog',
        canActivate: [permissionGuard],
        data: {
          permission: 'catalog:read',
          ...pending(
            'Semantic catalog',
            'The analyst will propose business definitions from your schema. Nothing becomes authoritative until an administrator approves it. Pending, rejected, and needs-review definitions stay out of answers.',
          ),
        },
        loadComponent: () => import('./features/pending/pending-page').then((m) => m.PendingPage),
      },
      {
        path: 'dashboards',
        canActivate: [permissionGuard],
        data: {
          permission: 'dashboards:read',
          ...pending('Dashboards', 'Saved questions and validated charts will be pinned here. There are no dashboards in this workspace yet.'),
        },
        loadComponent: () => import('./features/pending/pending-page').then((m) => m.PendingPage),
      },
      {
        path: 'history',
        canActivate: [permissionGuard],
        data: {
          permission: 'queries:read',
          ...pending('Query history', 'Every executed question will appear here with its validated SQL and status. No queries have been run.'),
        },
        loadComponent: () => import('./features/pending/pending-page').then((m) => m.PendingPage),
      },
      {
        path: 'team',
        canActivate: [permissionGuard],
        data: { permission: 'users:read' },
        loadComponent: () => import('./features/team/team-page').then((m) => m.TeamPage),
      },
      { path: 'settings', loadComponent: () => import('./features/settings/settings-page').then((m) => m.SettingsPage) },
      {
        path: 'billing',
        canActivate: [permissionGuard],
        data: { permission: 'billing:read' },
        loadComponent: () => import('./features/billing/billing-page').then((m) => m.BillingPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

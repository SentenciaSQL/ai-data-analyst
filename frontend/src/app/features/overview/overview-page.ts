import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Overview } from '../../core/auth/auth.models';
import { AuthStore } from '../../core/auth/auth.store';
import { apiMessage } from '../../core/forms/form-errors';
import { WorkspaceApi } from '../../core/api/workspace.api';

const ACTION_LABELS: Record<string, string> = {
  TENANT_REGISTERED: 'Workspace created',
  TENANT_UPDATED: 'Company name updated',
  USER_CREATED: 'Person added',
  USER_UPDATED: 'Person updated',
  USER_DISABLED: 'Person disabled',
  USER_LOGIN: 'Signed in',
  USER_LOGOUT: 'Signed out',
  USER_LOGIN_FAILED: 'Sign-in failed',
  PASSWORD_CHANGED: 'Password changed',
};

@Component({
  selector: 'app-overview-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './overview-page.html',
})
export class OverviewPage {
  private readonly api = inject(WorkspaceApi);
  readonly store = inject(AuthStore);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly overview = signal<Overview | null>(null);

  constructor() {
    this.api.overview().pipe(takeUntilDestroyed()).subscribe({
      next: (overview) => {
        this.overview.set(overview);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiMessage(error));
        this.loading.set(false);
      },
    });
  }

  actionLabel(action: string): string {
    return ACTION_LABELS[action] ?? action.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase());
  }
}

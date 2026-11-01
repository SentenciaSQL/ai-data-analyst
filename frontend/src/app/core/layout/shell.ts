import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthApi } from '../auth/auth.api';
import { AuthStore } from '../auth/auth.store';
import { ThemeStore } from '../theme/theme.store';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  permission?: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
})
export class Shell {
  private readonly auth = inject(AuthApi);
  private readonly router = inject(Router);
  readonly store = inject(AuthStore);
  readonly theme = inject(ThemeStore);
  readonly menuOpen = signal(false);
  readonly items: NavItem[] = [
    { label: 'Overview', path: '/overview', icon: 'pi pi-home' },
    { label: 'AI Analyst', path: '/analyst', icon: 'pi pi-comments', permission: 'conversations:read' },
    { label: 'Connections', path: '/connections', icon: 'pi pi-database', permission: 'connections:read' },
    { label: 'Semantic catalog', path: '/catalog', icon: 'pi pi-book', permission: 'catalog:read' },
    { label: 'Dashboards', path: '/dashboards', icon: 'pi pi-chart-bar', permission: 'dashboards:read' },
    { label: 'Query history', path: '/history', icon: 'pi pi-history', permission: 'queries:read' },
    { label: 'Team', path: '/team', icon: 'pi pi-users', permission: 'users:read' },
    { label: 'Settings', path: '/settings', icon: 'pi pi-cog' },
    { label: 'Billing', path: '/billing', icon: 'pi pi-credit-card', permission: 'billing:read' },
  ];
  readonly visibleItems = computed(() => this.items.filter((item) => !item.permission || this.store.has(item.permission)));

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  signOut(): void {
    this.auth.logout().subscribe({
      complete: () => this.finishSignOut(),
      error: () => this.finishSignOut(),
    });
  }

  private finishSignOut(): void {
    this.store.clear();
    void this.router.navigate(['/login']);
  }
}

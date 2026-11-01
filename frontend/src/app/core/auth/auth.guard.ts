import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth.store';

export const authGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  if (store.accessToken()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  if (!store.accessToken()) {
    return true;
  }
  return inject(Router).createUrlTree(['/overview']);
};

export const permissionGuard: CanActivateFn = (route) => {
  const permission = route.data['permission'] as string | undefined;
  const store = inject(AuthStore);
  if (!permission || store.has(permission)) {
    return true;
  }
  return inject(Router).createUrlTree(['/overview']);
};

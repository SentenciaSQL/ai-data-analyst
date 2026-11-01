import { ApplicationConfig, SecurityContext, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideMarkdown, SANITIZE } from 'ngx-markdown';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { catchError, finalize, firstValueFrom, of, tap } from 'rxjs';
import { routes } from './app.routes';
import { AuthApi } from './core/auth/auth.api';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthStore } from './core/auth/auth.store';
import { PRIMENG_LICENSE } from './primeng-license';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    providePrimeNG({
      ...(PRIMENG_LICENSE ? { license: PRIMENG_LICENSE } : {}),
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.dark',
          cssLayer: { name: 'primeng', order: 'theme, base, primeng' },
        },
      },
    }),
    provideMarkdown({
      sanitize: { provide: SANITIZE, useValue: SecurityContext.HTML },
    }),
    MessageService,
    provideAppInitializer(() => {
      const store = inject(AuthStore);
      const api = inject(AuthApi);
      if (!store.accessToken()) {
        store.markReady();
        return Promise.resolve();
      }
      return firstValueFrom(
        api.me().pipe(
          tap((profile) => store.setProfile(profile)),
          catchError(() => {
            store.clear();
            return of(null);
          }),
          finalize(() => store.markReady()),
        ),
      );
    }),
  ],
};

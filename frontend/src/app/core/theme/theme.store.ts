import { Injectable, effect, signal } from '@angular/core';

type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeStore {
  readonly theme = signal<Theme>(localStorage.getItem('ada.theme') === 'dark' ? 'dark' : 'light');

  constructor() {
    effect(() => {
      const dark = this.theme() === 'dark';
      document.documentElement.classList.toggle('dark', dark);
      localStorage.setItem('ada.theme', this.theme());
    });
  }

  toggle(): void {
    this.theme.update((theme) => (theme === 'dark' ? 'light' : 'dark'));
  }
}

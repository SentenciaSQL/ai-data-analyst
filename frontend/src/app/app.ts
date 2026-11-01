import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toast } from 'primeng/toast';
import { ThemeStore } from './core/theme/theme.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast],
  template: '<p-toast /><router-outlet />',
})
export class App {
  constructor() {
    inject(ThemeStore);
  }
}

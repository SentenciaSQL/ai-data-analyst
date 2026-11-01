import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-pending-page',
  template: `
    <header class="page-header">
      <h1>{{ title }}</h1>
      <p class="lede">{{ body }}</p>
    </header>
  `,
})
export class PendingPage {
  private readonly data = inject(ActivatedRoute).snapshot.data;
  readonly title = String(this.data['title'] ?? 'Workspace');
  readonly body = String(this.data['body'] ?? '');
}

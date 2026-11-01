import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MarkdownComponent } from 'ngx-markdown';
import { Button } from 'primeng/button';
import { Textarea } from 'primeng/textarea';
import { AuthApi } from '../../core/auth/auth.api';
import { AuthStore } from '../../core/auth/auth.store';
import { ChartSpec, parseChartSpec } from '../../shared/chart/chart-spec';
import { ChartView } from '../../shared/chart/chart-view';

interface Turn {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  chart: ChartSpec | null;
  sql: string | null;
}

const EXAMPLES = [
  'What were our top 10 selling products this month?',
  'Which customers generated the most revenue?',
  'How have our sales evolved over the last six months?',
  'What are our most profitable products?',
  'Show me the monthly revenue trend.',
];

@Component({
  selector: 'app-analyst-page',
  imports: [FormsModule, MarkdownComponent, Textarea, Button, ChartView],
  templateUrl: './analyst-page.html',
})
export class AnalystPage {
  private readonly auth = inject(AuthApi);
  readonly store = inject(AuthStore);
  readonly examples = EXAMPLES;
  readonly draft = signal('');
  readonly sending = signal(false);
  readonly status = signal('');
  readonly error = signal<string | null>(null);
  readonly turns = signal<Turn[]>([]);

  canAsk(): boolean {
    return this.store.has('conversations:write');
  }

  useExample(example: string): void {
    this.draft.set(example);
  }

  async ask(): Promise<void> {
    const message = this.draft().trim();
    if (!message || this.sending() || !this.canAsk()) {
      return;
    }
    const userId = crypto.randomUUID();
    const assistantId = crypto.randomUUID();
    this.turns.update((turns) => [
      ...turns,
      { id: userId, role: 'user', text: message, chart: null, sql: null },
      { id: assistantId, role: 'assistant', text: '', chart: null, sql: null },
    ]);
    this.draft.set('');
    this.sending.set(true);
    this.error.set(null);
    this.status.set('Sending the question');
    try {
      await this.stream(message, assistantId, false);
    } catch (error: unknown) {
      const text = error instanceof Error ? error.message : 'The analyst could not answer.';
      this.error.set(text);
      this.patch(assistantId, (turn) => ({ ...turn, text: turn.text || text }));
    } finally {
      this.sending.set(false);
      this.status.set('');
    }
  }

  private async stream(message: string, assistantId: string, retried: boolean): Promise<void> {
    const token = this.store.accessToken();
    const response = await fetch('/api/v1/analyst/messages:stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message }),
    });
    if (response.status === 401 && !retried) {
      await firstValueFrom(this.auth.refreshSession());
      return this.stream(message, assistantId, true);
    }
    if (!response.ok || !response.body) {
      throw new Error(await readError(response));
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
      const chunk = await reader.read();
      if (chunk.done) {
        break;
      }
      buffer += decoder.decode(chunk.value, { stream: true });
      const blocks = buffer.split('\n\n');
      buffer = blocks.pop() ?? '';
      for (const block of blocks) {
        this.applyBlock(block, assistantId);
      }
    }
    if (buffer.trim()) {
      this.applyBlock(buffer, assistantId);
    }
  }

  private applyBlock(block: string, assistantId: string): void {
    let event = 'message';
    const dataLines: string[] = [];
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim());
      }
    }
    if (dataLines.length === 0) {
      return;
    }
    let payload: Record<string, unknown> = {};
    try {
      const parsed: unknown = JSON.parse(dataLines.join('\n'));
      if (parsed && typeof parsed === 'object') {
        payload = parsed as Record<string, unknown>;
      }
    } catch {
      return;
    }
    if (event === 'token' && typeof payload['text'] === 'string') {
      const text = payload['text'];
      this.patch(assistantId, (turn) => ({ ...turn, text: turn.text + text }));
      return;
    }
    if (event === 'status') {
      this.status.set(statusLabel(payload));
      return;
    }
    if (event === 'error') {
      const message = typeof payload['message'] === 'string' ? payload['message'] : 'The analyst could not answer.';
      this.error.set(message);
      return;
    }
    if (event === 'chart') {
      const chart = parseChartSpec(payload['chart'] ?? payload);
      if (chart) {
        this.patch(assistantId, (turn) => ({ ...turn, chart }));
      }
      return;
    }
    if (event === 'sql' && typeof payload['sql'] === 'string') {
      const sql = payload['sql'];
      this.patch(assistantId, (turn) => ({ ...turn, sql }));
    }
  }

  private patch(id: string, update: (turn: Turn) => Turn): void {
    this.turns.update((turns) => turns.map((turn) => (turn.id === id ? update(turn) : turn)));
  }
}

function statusLabel(payload: Record<string, unknown>): string {
  const stage = typeof payload['stage'] === 'string' ? payload['stage'] : '';
  if (stage === 'classify_intent') return 'Identifying the question';
  if (stage === 'intent' && typeof payload['intent'] === 'string') return `Question type: ${payload['intent'].replaceAll('_', ' ')}`;
  if (stage === 'compose_answer') return 'Writing the answer';
  return stage.replaceAll('_', ' ');
}

async function readError(response: Response): Promise<string> {
  try {
    const body = await response.json() as { message?: string };
    if (typeof body.message === 'string' && body.message) {
      return body.message;
    }
  } catch {
    // The error body is not JSON.
  }
  return 'The analyst could not answer.';
}

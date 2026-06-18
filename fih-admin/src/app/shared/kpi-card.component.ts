import { Component, Input } from '@angular/core';
import { NumPipe } from './format';

/** A single KPI tile: icon, big number, one-line context. */
@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [NumPipe],
  template: `
    <div class="surface-card is-hoverable p-5 sm:p-6 flex flex-col gap-3 h-full">
      <div class="flex items-center justify-between">
        <span class="msr text-[22px]" [style.color]="accent ? 'var(--success)' : 'var(--primary)'">{{ icon }}</span>
        <span class="text-xs font-medium text-muted uppercase tracking-wide">{{ label }}</span>
      </div>
      <div class="text-3xl sm:text-4xl font-extrabold leading-none"
           [style.color]="accent ? 'var(--success)' : 'var(--ink)'">
        {{ isPercent ? display : (value | num) }}
      </div>
      <div class="text-sm text-muted">{{ context }}</div>
    </div>
  `
})
export class KpiCardComponent {
  @Input() icon = 'insights';
  @Input() label = '';
  @Input() value: number | null = 0;
  @Input() context = '';
  @Input() accent = false;
  @Input() isPercent = false;
  get display(): string { return this.value === null ? '—' : `${this.value.toFixed(1)}%`; }
}

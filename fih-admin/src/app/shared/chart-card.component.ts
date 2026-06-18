import { Component, Input } from '@angular/core';

/** A titled card that frames a chart (or any content) with consistent styling. */
@Component({
  selector: 'app-chart-card',
  standalone: true,
  template: `
    <div class="surface-card p-5 sm:p-6 h-full flex flex-col">
      <div class="mb-4">
        <h3 class="text-base font-semibold text-ink">{{ title }}</h3>
        @if (subtitle) { <p class="text-sm text-muted mt-0.5">{{ subtitle }}</p> }
      </div>
      <div class="flex-1 min-h-0">
        <ng-content></ng-content>
      </div>
    </div>
  `
})
export class ChartCardComponent {
  @Input() title = '';
  @Input() subtitle = '';
}

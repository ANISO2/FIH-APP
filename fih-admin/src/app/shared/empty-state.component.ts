import { Component, Input } from '@angular/core';

/** État vide / erreur convivial — jamais un écran blanc. */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="surface-card p-8 h-full flex flex-col items-center justify-center text-center gap-2">
      <span class="msr text-4xl" [style.color]="error ? 'var(--warn)' : 'var(--muted)'">{{ error ? 'error' : icon }}</span>
      <p class="font-medium text-ink">{{ title }}</p>
      <p class="text-sm text-muted max-w-xs">{{ message }}</p>
    </div>
  `
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'Aucun enregistrement trouvé';
  @Input() message = '';
  @Input() error = false;
}

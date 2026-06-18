import { Component, Input } from '@angular/core';

/** Shimmer placeholder shown while data loads. */
@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  template: `
    <div class="surface-card p-6 h-full">
      <div class="shimmer h-4 w-1/3 mb-4"></div>
      <div class="shimmer w-full" [style.height.px]="height"></div>
    </div>
  `
})
export class LoadingSkeletonComponent {
  @Input() height = 220;
}

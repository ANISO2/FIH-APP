import { Component, OnInit, signal } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { StatsService } from '../core/stats.service';
import { Gate } from '../core/models';
import { ChartCardComponent } from '../shared/chart-card.component';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, PctPipe } from '../shared/format';
import { BRAND } from '../shared/echarts-theme';

@Component({
  selector: 'app-gates',
  standalone: true,
  imports: [NgxEchartsDirective, ChartCardComponent, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, PctPipe],
  template: `
    <h2 class="text-xl font-bold text-ink mb-4">Portes</h2>

    @if (loading()) {
      <app-loading-skeleton [height]="320" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger les données des portes" message="Le serveur est peut-être indisponible." />
    } @else if (g()) {
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <div class="surface-card is-hoverable p-6">
          <div class="flex items-center gap-2 mb-3">
            <span class="msr text-primary">sensor_door</span>
            <h3 class="font-semibold text-ink">Porte Public</h3>
          </div>
          <div class="text-3xl font-extrabold">{{ g()!.public.scans | num }}</div>
          <div class="text-sm text-muted mt-1">
            <span style="color:var(--success)">{{ g()!.public.accepted | num }} acceptés</span> ·
            <span style="color:var(--warn)">{{ g()!.public.rejected | num }} refusés</span> ·
            {{ rate(g()!.public.accepted, g()!.public.scans) | pct }}
          </div>
        </div>
        <div class="surface-card is-hoverable p-6">
          <div class="flex items-center gap-2 mb-3">
            <span class="msr" style="color:var(--accent)">workspace_premium</span>
            <h3 class="font-semibold text-ink">Porte VIP</h3>
          </div>
          <div class="text-3xl font-extrabold">{{ g()!.vip.scans | num }}</div>
          <div class="text-sm text-muted mt-1">
            <span style="color:var(--success)">{{ g()!.vip.accepted | num }} acceptés</span> ·
            <span style="color:var(--warn)">{{ g()!.vip.rejected | num }} refusés</span> ·
            {{ rate(g()!.vip.accepted, g()!.vip.scans) | pct }}
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2">
          <app-chart-card title="Acceptés vs refusés par porte" subtitle="Totaux empilés">
            <div echarts theme="fih" [options]="stackOpt()" class="w-full h-[300px]"></div>
          </app-chart-card>
        </div>
        <div class="surface-card p-6 flex flex-col justify-center" style="background:#fbeae0;">
          <div class="flex items-center gap-2 mb-2">
            <span class="msr" style="color:var(--warn)">warning</span>
            <h3 class="font-semibold" style="color:var(--warn)">Scans refusés</h3>
          </div>
          <div class="text-4xl font-extrabold" style="color:var(--warn)">{{ totalRejected() | num }}</div>
          <p class="text-sm mt-2" style="color:#7a3a12">
            {{ rejectRate() | pct }} de tous les scans ont été refusés. Ce sont les entrées
            qui nécessitent une attention opérationnelle.
          </p>
        </div>
      </div>
    }
  `
})
export class GatesComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  g = signal<Gate | null>(null);
  stackOpt = signal<EChartsOption>({});

  constructor(private stats: StatsService) {}

  ngOnInit(): void { this.fetch(); }

  private reqId = 0;
  private fetch(): void {
    const seq = ++this.reqId;       // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    this.stats.gate().subscribe({
      next: (g) => { if (seq !== this.reqId) return; this.g.set(g); this.buildStack(g); this.loading.set(false); },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }

  rate(acc: number, total: number): number { return total ? Math.round(acc * 1000 / total) / 10 : 0; }
  totalRejected(): number { const g = this.g(); return g ? g.public.rejected + g.vip.rejected : 0; }
  rejectRate(): number {
    const g = this.g(); if (!g) return 0;
    const total = g.public.scans + g.vip.scans;
    return total ? Math.round(this.totalRejected() * 1000 / total) / 10 : 0;
  }

  private buildStack(g: Gate): void {
    this.stackOpt.set({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['Acceptés', 'Refusés'], top: 0 },
      xAxis: { type: 'category', data: ['Public', 'VIP'] },
      yAxis: { type: 'value' },
      series: [
        { name: 'Acceptés', type: 'bar', stack: 'total', itemStyle: { color: BRAND.success }, data: [g.public.accepted, g.vip.accepted] },
        { name: 'Refusés', type: 'bar', stack: 'total', itemStyle: { color: BRAND.warn }, data: [g.public.rejected, g.vip.rejected] }
      ]
    });
  }
}

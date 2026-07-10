import { Component, Input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { StatsService } from '../core/stats.service';
import { EventDetail } from '../core/models';
import { ChartCardComponent } from '../shared/chart-card.component';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, PctPipe, FDatePipe } from '../shared/format';
import { BRAND } from '../shared/echarts-theme';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [NgxEchartsDirective, ChartCardComponent, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, PctPipe, FDatePipe],
  template: `
    <button (click)="back()" class="flex items-center gap-1 text-sm text-muted hover:text-ink mb-4 transition-colors">
      <span class="msr text-[18px]">arrow_back</span> Retour aux événements
    </button>

    @if (loading()) {
      <app-loading-skeleton [height]="340" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger cet événement" message="Il n'existe peut-être pas, ou le serveur est indisponible." />
    } @else if (ev()) {
      <div class="surface-card p-6 mb-6">
        <h2 class="text-2xl font-bold text-ink">{{ ev()!.title }}</h2>
        <p class="text-muted">{{ ev()!.date | fdate }}</p>
        <div class="flex flex-wrap gap-2 mt-4">
          <span class="chip">{{ ev()!.scans | num }} entrées</span>
          <span class="chip" style="color:var(--success)">{{ ev()!.acceptanceRate | pct }} acceptés</span>
          <span class="chip">Public {{ ev()!.gate.public.scans | num }}</span>
          <span class="chip">VIP {{ ev()!.gate.vip.scans | num }}</span>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2">
          <app-chart-card title="Entrées par heure" subtitle="Le pic d'affluence de la soirée">
            <div echarts theme="fih" [options]="hourOpt()" class="w-full h-[300px]"></div>
          </app-chart-card>
        </div>
        <app-chart-card title="Répartition des portes" subtitle="Public vs VIP">
          <div echarts theme="fih" [options]="gateOpt()" class="w-full h-[300px]"></div>
        </app-chart-card>
      </div>
    }
  `,
  styles: [`.chip{background:var(--bg);border:1px solid var(--line);border-radius:999px;padding:6px 14px;font-size:13px;font-weight:600;color:var(--ink);}`]
})
export class EventDetailComponent implements OnInit {
  @Input() id!: string;   // lié depuis la route via withComponentInputBinding
  loading = signal(true);
  error = signal(false);
  ev = signal<EventDetail | null>(null);
  hourOpt = signal<EChartsOption>({});
  gateOpt = signal<EChartsOption>({});

  constructor(private stats: StatsService, private router: Router) {}

  ngOnInit(): void {
    const idNum = Number(this.id);
    this.stats.eventDetail(idNum).subscribe({
      next: (d) => { this.ev.set(d); this.buildHour(d); this.buildGate(d); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }

  private buildHour(d: EventDetail): void {
    this.hourOpt.set({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'category', data: d.entriesByHour.map(h => `${h.hour}:00`) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', itemStyle: { color: BRAND.primary, borderRadius: [4, 4, 0, 0] }, data: d.entriesByHour.map(h => h.scans) }]
    });
  }

  private buildGate(d: EventDetail): void {
    this.gateOpt.set({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['52%', '78%'], itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: [
          { value: d.gate.public.scans, name: 'Public', itemStyle: { color: BRAND.primary } },
          { value: d.gate.vip.scans, name: 'VIP', itemStyle: { color: BRAND.accent } }
        ]
      }]
    });
  }

  back(): void { this.router.navigate(['events']); }
}
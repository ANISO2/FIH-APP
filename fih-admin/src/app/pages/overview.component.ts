import { Component, OnInit, signal } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { forkJoin } from 'rxjs';
import type { EChartsOption } from 'echarts';
import { StatsService } from '../core/stats.service';
import { Overview, EntryByDay, Gate, TicketTypes, EventRollup } from '../core/models';
import { KpiCardComponent } from '../shared/kpi-card.component';
import { ChartCardComponent } from '../shared/chart-card.component';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { BRAND } from '../shared/echarts-theme';
import { realDate } from '../shared/format';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [NgxEchartsDirective, KpiCardComponent, ChartCardComponent, LoadingSkeletonComponent, EmptyStateComponent],
  template: `
    @if (loading()) {
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        @for (i of [1,2,3,4]; track i) { <app-loading-skeleton [height]="70" /> }
      </div>
      <app-loading-skeleton [height]="320" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger le tableau de bord"
                       message="Le serveur est peut-être indisponible ou votre session a expiré." />
    } @else if (ov()) {
      <!-- Ligne d'indicateurs -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <app-kpi-card icon="event" label="Événements" [value]="ov()!.totalEvents"
                      [context]="'Soirées du festival'" />
        <app-kpi-card icon="confirmation_number" label="Billets émis" [value]="ticketsIssued()"
                      [context]="'Billets + vouchers'" />
        <app-kpi-card icon="login" label="Entrées totales" [value]="ov()!.totalScans"
                      [context]="'Scans aux tourniquets'" />
        <app-kpi-card icon="verified" label="Taux d'acceptation" [value]="ov()!.acceptanceRate"
                      [isPercent]="true" [accent]="true"
                      [context]="(ov()!.acceptedScans.toLocaleString('fr-FR')) + ' acceptés'" />
      </div>

      <!-- Pièce maîtresse : fréquentation sur le festival -->
      <div class="mb-6">
        <app-chart-card title="Fréquentation sur le festival"
                        [subtitle]="'Scans acceptés vs refusés par soirée · plus forte affluence : ' + (ov()!.busiestEventTitle || '—')">
          <div echarts theme="fih" [options]="attendanceOpt()" class="w-full h-[340px]"></div>
        </app-chart-card>
      </div>

      <!-- Deuxième rangée de graphiques -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <app-chart-card title="Répartition des portes" subtitle="Public vs VIP">
          <div echarts theme="fih" [options]="gateOpt()" class="w-full h-[280px]"></div>
        </app-chart-card>
        <app-chart-card title="Billet vs Voucher" subtitle="Émis vs scannés">
          <div echarts theme="fih" [options]="ticketOpt()" class="w-full h-[280px]"></div>
        </app-chart-card>
        <app-chart-card title="Top événements" subtitle="Par fréquentation (top 8)">
          <div echarts theme="fih" [options]="topEventsOpt()" class="w-full h-[280px]"></div>
        </app-chart-card>
      </div>
    }
  `
})
export class OverviewComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  ov = signal<Overview | null>(null);

  attendanceOpt = signal<EChartsOption>({});
  gateOpt = signal<EChartsOption>({});
  ticketOpt = signal<EChartsOption>({});
  topEventsOpt = signal<EChartsOption>({});

  constructor(private stats: StatsService) {}

  ngOnInit(): void { this.fetch(); }

  ticketsIssued(): number {
    const o = this.ov();
    return o ? o.totalBillets + o.totalVouchers : 0;
  }

  private reqId = 0;
  private fetch(): void {
    const seq = ++this.reqId;          // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    forkJoin({
      overview: this.stats.overview(),
      byDay: this.stats.entriesByDay(),
      gate: this.stats.gate(),
      tickets: this.stats.ticketTypes(),
      events: this.stats.events()
    }).subscribe({
      next: (d) => {
        if (seq !== this.reqId) return;
        this.ov.set(d.overview);
        this.buildAttendance(d.byDay);
        this.buildGate(d.gate);
        this.buildTickets(d.tickets);
        this.buildTopEvents(d.events);
        this.loading.set(false);
      },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }

  private fmtDate(s: string): string {
    const d = realDate(s);
    return d ? d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }) : '—';
  }

  private buildAttendance(rows: EntryByDay[]): void {
    const dates = rows.map(r => this.fmtDate(r.date));
    this.attendanceOpt.set({
      tooltip: { trigger: 'axis' },
      legend: { data: ['Acceptés', 'Refusés'], right: 0, top: 0 },
      xAxis: { type: 'category', data: dates, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [
        {
          name: 'Acceptés', type: 'line', smooth: true, showSymbol: false,
          areaStyle: { opacity: 0.15 }, lineStyle: { width: 3 },
          itemStyle: { color: BRAND.primary }, data: rows.map(r => r.accepted)
        },
        {
          name: 'Refusés', type: 'line', smooth: true, showSymbol: false,
          areaStyle: { opacity: 0.12 }, lineStyle: { width: 2 },
          itemStyle: { color: BRAND.warn }, data: rows.map(r => r.rejected)
        }
      ]
    });
  }

  private buildGate(g: Gate): void {
    this.gateOpt.set({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['52%', '78%'], avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false }, labelLine: { show: false },
        data: [
          { value: g.public.scans, name: 'Public', itemStyle: { color: BRAND.primary } },
          { value: g.vip.scans, name: 'VIP', itemStyle: { color: BRAND.accent } }
        ]
      }]
    });
  }

  private buildTickets(t: TicketTypes): void {
    this.ticketOpt.set({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['Émis', 'Scannés'], top: 0 },
      xAxis: { type: 'category', data: ['Billet', 'Voucher'] },
      yAxis: { type: 'value' },
      series: [
        { name: 'Émis', type: 'bar', barGap: '10%', itemStyle: { color: BRAND.primary, borderRadius: [4, 4, 0, 0] }, data: [t.billet.issued, t.voucher.issued] },
        { name: 'Scannés', type: 'bar', itemStyle: { color: BRAND.accent, borderRadius: [4, 4, 0, 0] }, data: [t.billet.scanned, t.voucher.scanned] }
      ]
    });
  }

  private buildTopEvents(events: EventRollup[]): void {
    const top = [...events].sort((a, b) => b.scans - a.scans).slice(0, 8).reverse();
    this.topEventsOpt.set({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: top.map(e => e.title) },
      series: [{
        type: 'bar', itemStyle: { color: BRAND.primary, borderRadius: [0, 4, 4, 0] },
        data: top.map(e => e.scans)
      }]
    });
  }
}

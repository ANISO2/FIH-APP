import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../../app/theme.dart';
import '../../../../core/network/dio_client.dart';
import '../../data/stats_remote_data_source.dart';
import '../../data/stats_repository.dart';
import '../../domain/stats_models.dart';
import '../controllers/stats_controller.dart';

/// TODAY dashboard — how many people entered today, live. No global totals,
/// no money. Live by default; the "Geler" button freezes the snapshot until
/// you resume or restart the app.
class StatsDashboardPage extends StatefulWidget {
  const StatsDashboardPage({super.key});

  @override
  State<StatsDashboardPage> createState() => _StatsDashboardPageState();
}

class _StatsDashboardPageState extends State<StatsDashboardPage> {
  late final StatsController _c;
  static final NumberFormat _nf = NumberFormat.decimalPattern('fr_FR');
  static final DateFormat _hms = DateFormat('HH:mm:ss', 'fr_FR');
  static final DateFormat _long = DateFormat('EEEE d MMMM yyyy', 'fr_FR');
  static final DateFormat _dm = DateFormat('dd/MM', 'fr_FR');

  String _n(int v) => _nf.format(v);

  @override
  void initState() {
    super.initState();
    _c = StatsController(StatsRepository(StatsRemoteDataSource(DioClient.I.dio)));
    _c.init();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Aujourd'hui"),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [
          ListenableBuilder(
            listenable: _c,
            builder: (context, _) => IconButton(
              onPressed: _c.refresh,
              icon: const Icon(Icons.refresh_rounded),
              tooltip: 'Actualiser',
            ),
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: _c,
        builder: (context, _) {
          return Column(
            children: [
              _controlBar(),
              const Divider(height: 1),
              Expanded(child: _bodyFor(_c.state)),
            ],
          );
        },
      ),
    );
  }

  // ----------------------------------------------------------- control bar
  Widget _controlBar() {
    final live = _c.live;
    final Color dot = live
        ? (_c.offlineHint ? AppColors.verdictWarn : AppColors.verdictValid)
        : const Color(0xFF9AA7B2);
    final String label = !live
        ? 'GELÉ'
        : (_c.offlineHint ? 'HORS LIGNE' : 'EN DIRECT');
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Gap.md, vertical: Gap.sm),
      child: Row(
        children: [
          Container(width: 10, height: 10, decoration: BoxDecoration(color: dot, shape: BoxShape.circle)),
          const SizedBox(width: 6),
          Text(label, style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: dot)),
          const SizedBox(width: 10),
          if (_c.lastUpdated != null)
            Text('• ${_hms.format(_c.lastUpdated!)}',
                style: TextStyle(fontSize: 12, color: Colors.black.withValues(alpha: 0.5))),
          const Spacer(),
          // The freeze / resume button.
          OutlinedButton.icon(
            onPressed: () => _c.setLive(!live),
            icon: Icon(live ? Icons.pause_rounded : Icons.play_arrow_rounded, size: 18),
            label: Text(live ? 'Geler' : 'Direct'),
            style: OutlinedButton.styleFrom(
              foregroundColor: live ? AppColors.verdictStop : AppColors.verdictValid,
              side: BorderSide(color: live ? AppColors.verdictStop : AppColors.verdictValid),
              visualDensity: VisualDensity.compact,
            ),
          ),
        ],
      ),
    );
  }

  // ------------------------------------------------------------------ body
  Widget _bodyFor(StatsUiState state) {
    switch (state) {
      case StatsLoading():
        return const Center(child: CircularProgressIndicator());
      case StatsFailed(message: final m):
        return _errorView(m);
      case StatsLoaded(today: final t):
        return _dashboard(t);
    }
  }

  Widget _errorView(String message) => Center(
        child: Padding(
          padding: const EdgeInsets.all(Gap.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, size: 48, color: Color(0xFF9AA7B2)),
              const SizedBox(height: Gap.sm),
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: Gap.md),
              FilledButton.icon(
                onPressed: _c.refresh,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Réessayer'),
                style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
              ),
            ],
          ),
        ),
      );

  Widget _dashboard(TodayStats t) {
    return ListView(
      padding: const EdgeInsets.all(Gap.md),
      children: [
        if (!t.isToday)
          Container(
            width: double.infinity,
            margin: const EdgeInsets.only(bottom: Gap.md),
            padding: const EdgeInsets.all(Gap.sm),
            decoration: BoxDecoration(
              color: AppColors.verdictWarn.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                const Icon(Icons.info_outline_rounded, size: 18, color: AppColors.verdictWarn),
                const SizedBox(width: Gap.sm),
                Expanded(
                  child: Text(
                    "Aucune entrée aujourd'hui — dernier jour d'activité : ${_dm.format(t.day)}",
                    style: const TextStyle(fontSize: 13, color: AppColors.verdictWarn),
                  ),
                ),
              ],
            ),
          ),

        // Headline — entries.
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(Gap.md),
          decoration: BoxDecoration(color: AppColors.primary, borderRadius: BorderRadius.circular(16)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(t.isToday ? "Entrées aujourd'hui" : 'Entrées (${_dm.format(t.day)})',
                  style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
              const SizedBox(height: 4),
              Text(_n(t.entries),
                  style: const TextStyle(color: Colors.white, fontSize: 44, fontWeight: FontWeight.w800)),
              const SizedBox(height: 2),
              Text(_long.format(t.day),
                  style: const TextStyle(color: Colors.white70, fontSize: 12)),
            ],
          ),
        ),
        const SizedBox(height: Gap.md),

        // KPI row.
        Row(
          children: [
            Expanded(child: _kpi('Refusés', _n(t.rejected), Icons.block_rounded, AppColors.verdictStop)),
            const SizedBox(width: Gap.sm),
            Expanded(child: _kpi('Total scans', _n(t.total), Icons.qr_code_rounded, AppColors.primary)),
            const SizedBox(width: Gap.sm),
            Expanded(child: _kpi('Taux', '${t.rate.toStringAsFixed(1)} %', Icons.verified_rounded, AppColors.verdictValid)),
          ],
        ),

        const SizedBox(height: Gap.md),
        _section('Tendance (derniers jours)', _trendBars(t.trend)),

        const SizedBox(height: Gap.xl),
      ],
    );
  }

  // --------------------------------------------------------------- widgets
  Widget _section(String title, Widget child) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Gap.md),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title.toUpperCase(),
                style: const TextStyle(
                    color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: 13, letterSpacing: 0.5)),
            const SizedBox(height: Gap.sm),
            child,
          ],
        ),
      );

  Widget _kpi(String label, String value, IconData icon, Color color) => Container(
        padding: const EdgeInsets.all(Gap.sm),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: color, size: 20),
            const SizedBox(height: 6),
            Text(value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w800)),
            Text(label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 12, color: Colors.black.withValues(alpha: 0.55))),
          ],
        ),
      );

  Widget _trendBars(List<EntryByDay> days) {
    if (days.isEmpty) {
      return Text('Aucune donnée.', style: TextStyle(color: Colors.black.withValues(alpha: 0.5)));
    }
    final maxV = days.map((d) => d.scans).fold<int>(1, (a, b) => b > a ? b : a);
    const barArea = 110.0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          height: barArea + 36,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: days.map((d) {
                final acc = (d.accepted / maxV) * barArea;
                final rej = (d.rejected / maxV) * barArea;
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 5),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text(_n(d.accepted), style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 2),
                      SizedBox(
                        height: barArea,
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            if (rej > 0)
                              Container(width: 20, height: rej.clamp(2, barArea).toDouble(), color: AppColors.verdictStop.withValues(alpha: 0.85)),
                            Container(width: 20, height: acc.clamp(2, barArea).toDouble(), color: AppColors.primary),
                          ],
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(d.date == null ? '—' : _dm.format(d.date!), style: const TextStyle(fontSize: 10)),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
        ),
        const SizedBox(height: Gap.sm),
        Row(
          children: [
            _legendDot(AppColors.primary, 'Entrées'),
            const SizedBox(width: Gap.md),
            _legendDot(AppColors.verdictStop.withValues(alpha: 0.85), 'Refus'),
          ],
        ),
      ],
    );
  }

  Widget _legendDot(Color c, String label) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(width: 10, height: 10, decoration: BoxDecoration(color: c, shape: BoxShape.circle)),
          const SizedBox(width: 4),
          Text(label, style: const TextStyle(fontSize: 12)),
        ],
      );
}

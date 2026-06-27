import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../../app/theme.dart';
import '../../../../core/network/dio_client.dart';
import '../../data/stats_remote_data_source.dart';
import '../../data/stats_repository.dart';
import '../../domain/stats_models.dart';
import '../controllers/stats_controller.dart';

/// GLOBAL live dashboard for the verifier. Shows how many people entered,
/// acceptance rate, Public/VIP split, ticket types, and entries per day — no
/// money / recette (those stay in the backoffice). Auto-refresh is OFF until
/// the operator turns it on.
class StatsDashboardPage extends StatefulWidget {
  const StatsDashboardPage({super.key});

  @override
  State<StatsDashboardPage> createState() => _StatsDashboardPageState();
}

class _StatsDashboardPageState extends State<StatsDashboardPage> {
  late final StatsController _c;
  static final NumberFormat _nf = NumberFormat.decimalPattern('fr_FR');
  static final DateFormat _hms = DateFormat('HH:mm:ss', 'fr_FR');
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
        title: const Text('Statistiques'),
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
          final state = _c.state;
          return Column(
            children: [
              _controlBar(),
              const Divider(height: 1),
              Expanded(child: _bodyFor(state)),
            ],
          );
        },
      ),
    );
  }

  // ----------------------------------------------------------- control bar
  Widget _controlBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Gap.md, vertical: Gap.sm),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.event_rounded, size: 18, color: AppColors.primary),
              const SizedBox(width: 6),
              _yearDropdown(),
              const Spacer(),
              const Text('Auto 30 s', style: TextStyle(fontSize: 13)),
              Switch(
                value: _c.autoRefresh,
                onChanged: _c.setAutoRefresh,
                activeColor: AppColors.primary,
              ),
            ],
          ),
          Row(
            children: [
              Icon(
                _c.offlineHint ? Icons.cloud_off_rounded : Icons.schedule_rounded,
                size: 14,
                color: _c.offlineHint ? AppColors.verdictWarn : Colors.black.withValues(alpha: 0.45),
              ),
              const SizedBox(width: 6),
              Text(
                _c.lastUpdated == null
                    ? '—'
                    : (_c.offlineHint
                        ? 'Hors ligne — données de ${_hms.format(_c.lastUpdated!)}'
                        : 'Mis à jour à ${_hms.format(_c.lastUpdated!)}'),
                style: TextStyle(
                  fontSize: 12,
                  color: _c.offlineHint ? AppColors.verdictWarn : Colors.black.withValues(alpha: 0.55),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _yearDropdown() {
    return DropdownButtonHideUnderline(
      child: DropdownButton<int?>(
        value: _c.year,
        isDense: true,
        items: <DropdownMenuItem<int?>>[
          const DropdownMenuItem<int?>(value: null, child: Text('Toutes les années')),
          ..._c.years.map((y) => DropdownMenuItem<int?>(value: y, child: Text('$y'))),
        ],
        onChanged: (v) => _c.setYear(v),
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
      case StatsLoaded(data: final d):
        return _dashboard(d);
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

  Widget _dashboard(StatsDashboard d) {
    final o = d.overview;
    return ListView(
      padding: const EdgeInsets.all(Gap.md),
      children: [
        // Headline: entries + acceptance rate.
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(Gap.md),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Entrées (accès accordés)',
                  style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
              const SizedBox(height: 4),
              Text(_n(o.acceptedScans),
                  style: const TextStyle(color: Colors.white, fontSize: 40, fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              Text("Taux d'acceptation : ${o.acceptanceRate.toStringAsFixed(1)} %  ·  ${_n(o.totalScans)} scans",
                  style: const TextStyle(color: Colors.white70, fontSize: 13)),
            ],
          ),
        ),
        const SizedBox(height: Gap.md),

        // KPI grid.
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: Gap.sm,
          crossAxisSpacing: Gap.sm,
          childAspectRatio: 1.9,
          children: [
            _kpi('Refusés', _n(o.rejectedScans), Icons.block_rounded, AppColors.verdictStop),
            _kpi('Entrées Public', _n(o.publicScans), Icons.groups_rounded, AppColors.primary),
            _kpi('Entrées VIP', _n(o.vipScans), Icons.star_rounded, AppColors.accent),
            _kpi('Événements', _n(o.totalEvents), Icons.event_rounded, AppColors.primary),
            _kpi('Billets', _n(o.totalBillets), Icons.confirmation_number_rounded, AppColors.primary),
            _kpi('Vouchers', _n(o.totalVouchers), Icons.local_activity_rounded, AppColors.accent),
          ],
        ),

        if (o.busiestEventTitle != null) ...[
          const SizedBox(height: Gap.md),
          _card(
            child: Row(
              children: [
                const Icon(Icons.local_fire_department_rounded, color: AppColors.verdictWarn),
                const SizedBox(width: Gap.sm),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Spectacle le plus fréquenté',
                          style: TextStyle(fontSize: 12, color: Colors.black54)),
                      Text(o.busiestEventTitle!,
                          style: const TextStyle(fontWeight: FontWeight.w700)),
                    ],
                  ),
                ),
                Text('${_n(o.busiestEventScans)} entrées',
                    style: const TextStyle(fontWeight: FontWeight.w700, color: AppColors.primary)),
              ],
            ),
          ),
        ],

        const SizedBox(height: Gap.md),
        _section('Entrées par jour', _dayBars(d.entriesByDay)),

        const SizedBox(height: Gap.md),
        _section('Par porte', Column(
          children: [
            _gateRow('Public', d.gate.publicGate),
            const SizedBox(height: Gap.sm),
            _gateRow('VIP', d.gate.vip),
          ],
        )),

        const SizedBox(height: Gap.md),
        _section('Par type', Column(
          children: [
            _typeRow('Billets', d.ticketTypes.billet),
            const SizedBox(height: Gap.sm),
            _typeRow('Vouchers', d.ticketTypes.voucher),
          ],
        )),

        const SizedBox(height: Gap.xl),
      ],
    );
  }

  // --------------------------------------------------------------- widgets
  Widget _card({required Widget child}) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Gap.md),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
        child: child,
      );

  Widget _section(String title, Widget child) => _card(
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
        child: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(color: color.withValues(alpha: 0.12), shape: BoxShape.circle),
              child: Icon(icon, color: color, size: 20),
            ),
            const SizedBox(width: Gap.sm),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(value,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                  Text(label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 12, color: Colors.black.withValues(alpha: 0.55))),
                ],
              ),
            ),
          ],
        ),
      );

  Widget _gateRow(String label, GateBucket b) {
    final total = b.scans == 0 ? 1 : b.scans;
    final accFrac = b.accepted / total;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
            const Spacer(),
            Text('${_n(b.accepted)} ✓   ${_n(b.rejected)} ✗',
                style: TextStyle(fontSize: 13, color: Colors.black.withValues(alpha: 0.6))),
          ],
        ),
        const SizedBox(height: 4),
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: Row(
            children: [
              Expanded(
                flex: (accFrac * 1000).round().clamp(0, 1000).toInt(),
                child: Container(height: 8, color: AppColors.verdictValid),
              ),
              Expanded(
                flex: (1000 - (accFrac * 1000).round()).clamp(0, 1000).toInt(),
                child: Container(height: 8, color: AppColors.verdictStop.withValues(alpha: 0.85)),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _typeRow(String label, TicketBucket b) {
    final double frac = b.issued == 0 ? 0.0 : (b.scanned / b.issued).clamp(0.0, 1.0).toDouble();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
            const Spacer(),
            Text('${_n(b.scanned)} scannés / ${_n(b.issued)} émis',
                style: TextStyle(fontSize: 13, color: Colors.black.withValues(alpha: 0.6))),
          ],
        ),
        const SizedBox(height: 4),
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: LinearProgressIndicator(
            value: frac,
            minHeight: 8,
            backgroundColor: AppColors.primary.withValues(alpha: 0.12),
            valueColor: const AlwaysStoppedAnimation(AppColors.primary),
          ),
        ),
      ],
    );
  }

  Widget _dayBars(List<EntryByDay> days) {
    if (days.isEmpty) {
      return Text('Aucune entrée enregistrée.',
          style: TextStyle(color: Colors.black.withValues(alpha: 0.5)));
    }
    final maxV = days.map((d) => d.scans).fold<int>(1, (a, b) => b > a ? b : a);
    const barArea = 110.0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          height: barArea + 26,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: days.map((d) {
                final acc = (d.accepted / maxV) * barArea;
                final rej = (d.rejected / maxV) * barArea;
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      SizedBox(
                        height: barArea,
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            if (rej > 0)
                              Container(width: 16, height: rej.clamp(2, barArea).toDouble(), color: AppColors.verdictStop.withValues(alpha: 0.85)),
                            Container(width: 16, height: acc.clamp(2, barArea).toDouble(), color: AppColors.primary),
                          ],
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(d.date == null ? '—' : _dm.format(d.date!),
                          style: const TextStyle(fontSize: 10)),
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

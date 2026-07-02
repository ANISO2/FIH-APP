import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../../app/theme.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../../data/stats_remote_data_source.dart';
import '../../data/stats_repository.dart';
import '../../domain/tourniquet_models.dart';

/// "Statistique des tourniquets" — the same information the backoffice shows,
/// scoped to the SINGLE day the dashboard is showing (today, or its latest
/// active-day fallback). Reads the existing read-only GET /api/stats/tourniquets
/// feed (device-token allowed) and filters to [day].
class TourniquetsPage extends StatefulWidget {
  final DateTime day;
  final bool isToday;
  const TourniquetsPage({super.key, required this.day, this.isToday = true});

  @override
  State<TourniquetsPage> createState() => _TourniquetsPageState();
}

class _TourniquetsPageState extends State<TourniquetsPage> {
  late final StatsRepository _repo;
  late Future<List<TourniquetEvent>> _future;

  static final NumberFormat _nf = NumberFormat.decimalPattern('fr_FR');
  static final NumberFormat _pct = NumberFormat('#0.0', 'fr_FR');
  static final DateFormat _dm = DateFormat('d MMM', 'fr_FR');

  String _n(num v) => _nf.format(v);

  @override
  void initState() {
    super.initState();
    _repo = StatsRepository(StatsRemoteDataSource(DioClient.I.dio));
    _load();
  }

  void _load() {
    _future = _repo.tourniquets(widget.day.year);
  }

  void _reload() => setState(_load);

  bool _sameDay(DateTime? a, DateTime b) =>
      a != null && a.year == b.year && a.month == b.month && a.day == b.day;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Statistique des tourniquets'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            onPressed: _reload,
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Actualiser',
          ),
        ],
      ),
      body: FutureBuilder<List<TourniquetEvent>>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            final msg = snap.error is ApiException
                ? (snap.error as ApiException).frenchHint
                : 'Impossible de charger les statistiques.';
            return _error(msg);
          }
          final events =
              (snap.data ?? const []).where((e) => _sameDay(e.eventDate, widget.day)).toList();
          if (events.isEmpty) return _empty();

          return ListView(
            padding: const EdgeInsets.all(Gap.md),
            children: [
              _subtitle(),
              const SizedBox(height: Gap.sm),
              for (final e in events) ...[
                _eventCard(e),
                const SizedBox(height: Gap.md),
              ],
              const SizedBox(height: Gap.lg),
            ],
          );
        },
      ),
    );
  }

  Widget _subtitle() => Text(
        'Codes accessibles et transactions par spectacle et modèle · ${widget.day.year}',
        style: TextStyle(fontSize: 12, color: Colors.black.withValues(alpha: 0.55)),
      );

  Widget _error(String message) => Center(
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
                onPressed: _reload,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Réessayer'),
                style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
              ),
            ],
          ),
        ),
      );

  Widget _empty() => Center(
        child: Padding(
          padding: const EdgeInsets.all(Gap.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.event_busy_rounded, size: 48, color: Color(0xFF9AA7B2)),
              const SizedBox(height: Gap.sm),
              Text('Aucune donnée pour le ${_dm.format(widget.day)} ${widget.day.year}.',
                  textAlign: TextAlign.center),
            ],
          ),
        ),
      );

  // -------------------------------------------------------------- event card
  Widget _eventCard(TourniquetEvent e) => Card(
        child: Padding(
          padding: const EdgeInsets.all(Gap.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Title + date.
              Row(
                children: [
                  Flexible(
                    child: Text(e.eventTitle,
                        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
                  ),
                  const SizedBox(width: Gap.sm),
                  if (e.eventDate != null)
                    Text(_dm.format(e.eventDate!),
                        style: TextStyle(fontSize: 13, color: Colors.black.withValues(alpha: 0.5))),
                ],
              ),
              const SizedBox(height: Gap.sm),
              // Header metric band (matches the backoffice top row).
              Wrap(
                spacing: Gap.md,
                runSpacing: Gap.sm,
                children: [
                  _metric('Émis', _n(e.audience)),
                  _metric('Transactions Billets', _n(e.transactionsBillets)),
                  _metric('Transactions Vouchers', _n(e.transactionsVouchers)),
                  _metric('Entrées', _n(e.tourniquets)),
                  _presenceMetric(e.presence),
                ],
              ),
              const SizedBox(height: Gap.md),
              const Divider(height: 1),
              const SizedBox(height: Gap.sm),
              _modelTable(e.rows),
            ],
          ),
        ),
      );

  Widget _metric(String label, String value) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label, style: TextStyle(fontSize: 11, color: Colors.black.withValues(alpha: 0.5))),
          const SizedBox(height: 2),
          Text(value, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
        ],
      );

  Widget _presenceMetric(double pct) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Présence', style: TextStyle(fontSize: 11, color: Colors.black.withValues(alpha: 0.5))),
          const SizedBox(height: 4),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 48,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: (pct / 100).clamp(0.0, 1.0),
                    minHeight: 8,
                    backgroundColor: AppColors.primary.withValues(alpha: 0.15),
                    valueColor: const AlwaysStoppedAnimation(AppColors.primary),
                  ),
                ),
              ),
              const SizedBox(width: 6),
              Text('${_pct.format(pct)} %',
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w800)),
            ],
          ),
        ],
      );

  // -------------------------------------------------------------- model table
  // Two grouped column families, like the backoffice:
  //   "Code à barre accessibles" -> Billet / Voucher / Audience
  //   "Transactions tourniquet"  -> Billet / Voucher
  // Horizontally scrollable so it stays readable on a narrow phone.
  static const double _wName = 132;
  static const double _wNum = 62;

  Widget _modelTable(List<TourniquetRow> rows) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Group header.
          Row(
            children: [
              const SizedBox(width: _wName),
              _group('Code à barre accessibles', _wNum * 3),
              _group('Transactions tourniquet', _wNum * 2),
            ],
          ),
          const SizedBox(height: 4),
          // Sub header.
          Row(
            children: [
              _cell('Modèle', _wName, header: true, align: TextAlign.left),
              _cell('Billet', _wNum, header: true),
              _cell('Voucher', _wNum, header: true),
              _cell('Audience', _wNum, header: true),
              _cell('Billet', _wNum, header: true),
              _cell('Voucher', _wNum, header: true),
            ],
          ),
          const Divider(height: 12),
          // Data rows.
          for (final r in rows)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 3),
              child: Row(
                children: [
                  _cell(r.modelName, _wName, align: TextAlign.left, bold: true),
                  _cell(_n(r.billetCodes), _wNum),
                  _cell(_n(r.voucherCodes), _wNum),
                  _cell(_n(r.audience), _wNum),
                  _cell(_n(r.billetTransactions), _wNum, color: AppColors.primary),
                  _cell(_n(r.voucherTransactions), _wNum, color: AppColors.verdictWarn),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _group(String label, double width) => Container(
        width: width,
        padding: const EdgeInsets.symmetric(vertical: 4),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(6),
        ),
        child: Text(label,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontSize: 11, fontWeight: FontWeight.w700, color: AppColors.primary)),
      );

  Widget _cell(String text, double width,
      {bool header = false, bool bold = false, Color? color, TextAlign align = TextAlign.right}) {
    return SizedBox(
      width: width,
      child: Text(
        text,
        textAlign: align,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: header ? 11 : 13,
          fontWeight: header ? FontWeight.w600 : (bold ? FontWeight.w700 : FontWeight.w500),
          color: header
              ? Colors.black.withValues(alpha: 0.55)
              : (color ?? Colors.black.withValues(alpha: 0.85)),
        ),
      ),
    );
  }
}

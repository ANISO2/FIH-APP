import 'package:flutter/material.dart';

import '../../../../app/theme.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/utils/formatters.dart';
import '../../data/verification_remote_data_source.dart';
import '../../data/verification_repository.dart';
import '../../domain/access_log.dart';
import '../../domain/ticket_details.dart';
import '../../domain/verification_result.dart';
import '../../domain/voucher_info.dart';
import '../widgets/status_pills.dart';

/// ℹ details screen. Loaded lazily from the scan result: identity + status
/// pills (already known from the scan), then the access history (Public +
/// VIP), management extras, and — for vouchers — the external commercial info.
class TicketDetailsPage extends StatefulWidget {
  final VerificationResult result;
  const TicketDetailsPage({super.key, required this.result});

  @override
  State<TicketDetailsPage> createState() => _TicketDetailsPageState();
}

class _TicketDetailsPageState extends State<TicketDetailsPage> {
  late final VerificationRepository _repo;
  late Future<TicketDetails> _details;
  Future<VoucherInfo>? _externalInfo;

  bool get _isVoucher => widget.result.type == 'VOUCHER';

  @override
  void initState() {
    super.initState();
    _repo = VerificationRepository(VerificationRemoteDataSource(DioClient.I.dio));
    final ns = widget.result.numeroserie ?? widget.result.codebarre ?? '';
    _details = _repo.ticketDetails(widget.result.type, ns);
    if (_isVoucher && (widget.result.codebarre ?? '').isNotEmpty) {
      _externalInfo = _repo.voucherInfo(widget.result.codebarre!);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Détails'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(Gap.md),
        children: [
          _identityCard(),
          const SizedBox(height: Gap.md),
          FutureBuilder<TicketDetails>(
            future: _details,
            builder: (context, snap) {
              if (snap.connectionState != ConnectionState.done) {
                return const _Loading();
              }
              if (snap.hasError) {
                return _ErrorCard(
                  message: (snap.error is ApiException)
                      ? (snap.error as ApiException).frenchHint
                      : 'Impossible de charger les détails.',
                  onRetry: () => setState(() {
                    final ns = widget.result.numeroserie ?? widget.result.codebarre ?? '';
                    _details = _repo.ticketDetails(widget.result.type, ns);
                  }),
                );
              }
              final d = snap.data!;
              return Column(
                children: [
                  _extrasCard(d),
                  const SizedBox(height: Gap.md),
                  _LogSection(title: 'Accès — Public', entries: d.accessPublic),
                  const SizedBox(height: Gap.md),
                  _LogSection(title: 'Accès — VIP', entries: d.accessVip),
                ],
              );
            },
          ),
          if (_isVoucher) ...[
            const SizedBox(height: Gap.md),
            _externalCard(),
          ],
          const SizedBox(height: Gap.xl),
        ],
      ),
    );
  }

  // -------------------------------------------------------------- identity
  Widget _identityCard() {
    final r = widget.result;
    final f = r.flags;
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _typeChip(r.type),
              if (r.ticketModel != null) ...[
                const SizedBox(width: Gap.sm),
                Flexible(child: _softChip(r.ticketModel!)),
              ],
            ],
          ),
          const SizedBox(height: Gap.sm),
          if (r.eventTitle != null)
            Text(r.eventTitle!, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          if (r.eventDate != null)
            Text(Formatters.longDate(r.eventDate),
                style: TextStyle(color: Colors.black.withValues(alpha: 0.6))),
          const SizedBox(height: Gap.sm),
          _kv('N° série', r.numeroserie ?? '—'),
          _kv('Code à barre', r.codebarre ?? '—'),
          const Divider(height: Gap.lg),
          StatusPillRow(
            vente: f.vendu,
            utilisation: f.utilisation,
            reservation: f.reservation,
            activation: f.activation,
            cancelled: f.cancelled,
          ),
        ],
      ),
    );
  }

  // ----------------------------------------------------------------- extras
  Widget _extrasCard(TicketDetails d) {
    final rows = <Widget>[];
    if (_isVoucher) {
      rows.add(_kv('Commande', d.commande?.toString() ?? '—'));
      rows.add(_kv('Date de vente', Formatters.shortDate(d.dateVente)));
    } else {
      rows.add(_kv('Livré', d.livre == null ? '—' : (d.livre! ? 'Oui' : 'Non')));
      rows.add(_kv('Date de livraison', Formatters.shortDate(d.dateLivraison)));
      rows.add(_kv('Date de vente', Formatters.shortDate(d.dateVente)));
    }
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [_sectionTitle('Informations'), const SizedBox(height: Gap.xs), ...rows],
      ),
    );
  }

  // --------------------------------------------------------------- external
  Widget _externalCard() {
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _sectionTitle('Informations commerciales'),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.accent.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Text('Service externe',
                    style: TextStyle(color: AppColors.accent, fontSize: 12, fontWeight: FontWeight.w700)),
              ),
            ],
          ),
          const SizedBox(height: Gap.sm),
          FutureBuilder<VoucherInfo>(
            future: _externalInfo,
            builder: (context, snap) {
              if (_externalInfo == null) {
                return _hint('Code à barre manquant pour interroger le service externe.');
              }
              if (snap.connectionState != ConnectionState.done) {
                return const Padding(
                  padding: EdgeInsets.symmetric(vertical: Gap.md),
                  child: Center(child: CircularProgressIndicator()),
                );
              }
              if (snap.hasError) {
                return _hint('Service externe injoignable pour le moment.');
              }
              final info = snap.data!;
              if (info.isPending) {
                return Row(
                  children: [
                    const Icon(Icons.cloud_sync_rounded, color: AppColors.accent),
                    const SizedBox(width: Gap.sm),
                    Expanded(
                      child: Text(
                        info.message ?? 'Intégration à venir — vérification déléguée au service externe.',
                        style: TextStyle(color: Colors.black.withValues(alpha: 0.7)),
                      ),
                    ),
                  ],
                );
              }
              if (info.isNotFound) {
                return _hint('Voucher inconnu du service externe.');
              }
              return Column(
                children: [
                  _kv('Modèle', info.model ?? '—'),
                  _kv('Prix', Formatters.money(info.prix)),
                  _kv('Vendu', info.vendu == null ? '—' : (info.vendu! ? 'Oui' : 'Non')),
                  _kv('Date de vente', Formatters.shortDate(info.dateVente)),
                  _kv('Compteur d\'accès', info.accessCounter?.toString() ?? '—'),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  // ------------------------------------------------------------- primitives
  Widget _card({required Widget child}) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Gap.md),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
        child: child,
      );

  Widget _sectionTitle(String t) => Text(t.toUpperCase(),
      style: const TextStyle(
          color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: 13, letterSpacing: 0.5));

  Widget _kv(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 3),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
                width: 132,
                child: Text(k, style: TextStyle(color: Colors.black.withValues(alpha: 0.55)))),
            Expanded(child: Text(v, style: const TextStyle(fontWeight: FontWeight.w600))),
          ],
        ),
      );

  Widget _hint(String t) =>
      Text(t, style: TextStyle(color: Colors.black.withValues(alpha: 0.6)));

  Widget _typeChip(String type) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
        decoration: BoxDecoration(color: AppColors.primary, borderRadius: BorderRadius.circular(20)),
        child: Text(type == 'BILLET' ? 'Billet' : 'Voucher',
            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 13)),
      );

  Widget _softChip(String label) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: AppColors.primary.withValues(alpha: 0.08),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(label,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w600, fontSize: 13)),
      );
}

/// One access-log section (Public or VIP) — a header with a count and a list of
/// passages, or an empty hint.
class _LogSection extends StatelessWidget {
  final String title;
  final List<AccessLogEntry> entries;
  const _LogSection({required this.title, required this.entries});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(Gap.md),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(title.toUpperCase(),
                  style: const TextStyle(
                      color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: 13, letterSpacing: 0.5)),
              const SizedBox(width: Gap.sm),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(20)),
                child: Text('${entries.length}',
                    style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: 12)),
              ),
            ],
          ),
          const SizedBox(height: Gap.sm),
          if (entries.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: Gap.sm),
              child: Text('Aucun passage enregistré.',
                  style: TextStyle(color: Colors.black.withValues(alpha: 0.5))),
            )
          else
            ...entries.map((e) => _LogTile(entry: e)),
        ],
      ),
    );
  }
}

class _LogTile extends StatelessWidget {
  final AccessLogEntry entry;
  const _LogTile({required this.entry});

  @override
  Widget build(BuildContext context) {
    final color = entry.granted ? AppColors.verdictValid : AppColors.verdictStop;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 34,
            height: 34,
            decoration: BoxDecoration(color: color.withValues(alpha: 0.12), shape: BoxShape.circle),
            child: Icon(entry.granted ? Icons.check_rounded : Icons.close_rounded, color: color, size: 20),
          ),
          const SizedBox(width: Gap.sm),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(entry.porte ?? '—', style: const TextStyle(fontWeight: FontWeight.w700)),
                    const Spacer(),
                    Text(Formatters.time(entry.time),
                        style: TextStyle(color: Colors.black.withValues(alpha: 0.6), fontSize: 13)),
                  ],
                ),
                Text(
                  '${Formatters.dayMonth(entry.date)}  ·  Réf ${entry.reference ?? '—'}',
                  style: TextStyle(color: Colors.black.withValues(alpha: 0.55), fontSize: 12),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Loading extends StatelessWidget {
  const _Loading();
  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.symmetric(vertical: Gap.xl),
        child: Center(child: CircularProgressIndicator()),
      );
}

class _ErrorCard extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorCard({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(Gap.md),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
      child: Column(
        children: [
          const Icon(Icons.cloud_off_rounded, size: 40, color: Color(0xFF9AA7B2)),
          const SizedBox(height: Gap.sm),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: Gap.sm),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Réessayer'),
          ),
        ],
      ),
    );
  }
}

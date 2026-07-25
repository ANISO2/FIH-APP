import 'package:flutter/material.dart';

import '../../../../app/theme.dart';
import '../../domain/voucher_info.dart';

/// Feature 1 — holder / used status for a scanned voucher, from the EXTERNAL
/// ticket-verify service. Not a verdict screen (no pass/no-pass): verification
/// isn't ours. On success we show the returned info; on failure we show a plain
/// French message instead of the old "intégration à venir" placeholder.
class VoucherInfoView extends StatelessWidget {
  final VoucherInfo info;
  final VoidCallback onNext;

  const VoucherInfoView({super.key, required this.info, required this.onNext});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.surface,
      child: SafeArea(
        child: Column(
          children: [
            _Header(),
            Expanded(child: _body(context)),
            Padding(
              padding: const EdgeInsets.all(Gap.md),
              child: FilledButton.icon(
                onPressed: onNext,
                icon: const Icon(Icons.qr_code_scanner_rounded),
                label: const Text('Scanner suivant'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _body(BuildContext context) {
    if (info.isNotFound) {
      return const _EmptyState(
        icon: Icons.search_off_rounded,
        title: 'Billet introuvable',
        message: 'Le service externe ne connaît pas ce code.',
      );
    }
    if (info.isUnavailable) {
      return _EmptyState(
        icon: Icons.cloud_off_rounded,
        title: 'Service indisponible',
        message: info.message ??
            'Le service de vérification est momentanément indisponible. Réessayez.',
      );
    }
    // OK or ERROR — both may carry holder data; ERROR leads with the message.
    return SingleChildScrollView(
      padding: const EdgeInsets.all(Gap.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _usedBanner(),
          if (info.isError && (info.message?.isNotEmpty ?? false))
            Container(
              margin: const EdgeInsets.only(bottom: Gap.md),
              padding: const EdgeInsets.all(Gap.md),
              decoration: BoxDecoration(
                color: AppColors.verdictStop.withValues(alpha: 0.10),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Row(
                children: [
                  const Icon(Icons.error_outline_rounded, color: AppColors.verdictStop),
                  const SizedBox(width: Gap.sm),
                  Expanded(
                    child: Text(info.message!,
                        style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.verdictStop)),
                  ),
                ],
              ),
            ),
          Container(
            padding: const EdgeInsets.all(Gap.md),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (info.holder != null) ...[
                  Text(info.holder!,
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
                  const Divider(height: Gap.lg),
                ],
                _kv('Utilisé', info.used == null ? '—' : (info.used! ? 'Oui' : 'Non')),
                _kv("Date d'utilisation", _orDash(info.usedDate)),
                _kv('Ticket', _orDash(info.ticket)),
                _kv('CIN', _orDash(info.ticketCin)),
                _kv('Prénom', _orDash(info.prenom)),
                _kv('Nom', _orDash(info.nom)),
                if (info.code != null) _kv('Code', info.code!),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Prominent used / not-used chip so an operator sees re-entry at a glance.
  Widget _usedBanner() {
    if (info.used == null) return const SizedBox.shrink();
    final used = info.used!;
    final color = used ? AppColors.verdictWarn : AppColors.verdictValid;
    return Container(
      margin: const EdgeInsets.only(bottom: Gap.md),
      padding: const EdgeInsets.symmetric(vertical: Gap.sm, horizontal: Gap.md),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(used ? Icons.history_toggle_off_rounded : Icons.check_circle_rounded, color: color),
          const SizedBox(width: Gap.sm),
          Text(used ? 'Déjà utilisé' : 'Non utilisé',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: color)),
          if (used && (info.usedDate?.isNotEmpty ?? false)) ...[
            const Spacer(),
            Text(info.usedDate!, style: TextStyle(fontSize: 12, color: color)),
          ],
        ],
      ),
    );
  }

  static String _orDash(String? v) => (v == null || v.trim().isEmpty) ? '—' : v;

  Widget _kv(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 5),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 130,
              child: Text(k, style: TextStyle(color: Colors.black.withValues(alpha: 0.55))),
            ),
            Expanded(child: Text(v, style: const TextStyle(fontWeight: FontWeight.w600))),
          ],
        ),
      );
}

class _Header extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(Gap.md, Gap.md, Gap.md, Gap.md),
      child: Row(
        children: [
          const Icon(Icons.confirmation_number_outlined, color: Colors.white),
          const SizedBox(width: Gap.sm),
          const Text('Info voucher',
              style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w700)),
          const Spacer(),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.18),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Text('Service externe',
                style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;
  const _EmptyState({required this.icon, required this.title, required this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(Gap.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 88, color: AppColors.accent),
            const SizedBox(height: Gap.md),
            Text(title,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w700)),
            const SizedBox(height: Gap.sm),
            Text(message,
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16, color: Colors.black.withValues(alpha: 0.6))),
          ],
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../../../../app/theme.dart';
import '../../../../core/utils/formatters.dart';
import '../../domain/voucher_info.dart';

/// Feature 1 — commercial info about a paid voucher, from the EXTERNAL service.
/// Deliberately NOT a verdict screen: no pass/no-pass, no alarm — verification
/// isn't ours (source = EXTERNAL_SERVICE). Today the service is a stub, so the
/// star of the show is a first-class "Intégration à venir" state, not an error.
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
    if (info.isPending) {
      return _EmptyState(
        icon: Icons.cloud_sync_rounded,
        title: 'Intégration à venir',
        message: info.message ??
            'Vérification déléguée au service externe (équipe billetterie). '
                'Le contrat de réponse est déjà figé, l\'écran est prêt.',
      );
    }
    if (info.isNotFound) {
      return const _EmptyState(
        icon: Icons.search_off_rounded,
        title: 'Voucher introuvable',
        message: 'Le service externe ne connaît pas ce code.',
      );
    }
    // status == OK
    return SingleChildScrollView(
      padding: const EdgeInsets.all(Gap.md),
      child: Container(
        padding: const EdgeInsets.all(Gap.md),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (info.eventTitle != null)
              Text(info.eventTitle!,
                  style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
            if (info.eventDate != null)
              Padding(
                padding: const EdgeInsets.only(top: 2, bottom: Gap.sm),
                child: Text(Formatters.longDate(info.eventDate),
                    style: TextStyle(color: Colors.black.withValues(alpha: 0.6))),
              ),
            const Divider(height: Gap.lg),
            _kv('Modèle', info.model ?? '—'),
            _kv('Prix', Formatters.money(info.prix)),
            _kv('Vendu', info.vendu == null ? '—' : (info.vendu! ? 'Oui' : 'Non')),
            _kv('Date de vente', Formatters.shortDate(info.dateVente)),
            _kv('Compteur d\'accès', info.accessCounter?.toString() ?? '—'),
            _kv('N° série', info.numeroserie ?? '—'),
            _kv('Code-barres', info.codebarre ?? '—'),
          ],
        ),
      ),
    );
  }

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

import 'package:flutter/material.dart';

import '../../../../app/theme.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/utils/formatters.dart';
import '../../domain/verdict.dart';
import '../../domain/verification_result.dart';
import 'status_pills.dart';

/// Full-bleed verdict screen: the whole screen becomes the answer (color + word
/// + gate instruction). Below it, a card mirrors the backoffice row — status
/// pills + key fields — and a "Plus de détails" button opens the ℹ screen.
class VerdictResultView extends StatelessWidget {
  final VerificationResult result;
  final VoidCallback onNext;
  final VoidCallback? onDetails;

  const VerdictResultView({
    super.key,
    required this.result,
    required this.onNext,
    this.onDetails,
  });

  @override
  Widget build(BuildContext context) {
    final view = VerdictView.of(result.verdict);
    return Container(
      color: view.color,
      child: SafeArea(
        child: Column(
          children: [
            const SizedBox(height: Gap.lg),
            Icon(view.icon, size: 96, color: AppColors.onVerdict),
            const SizedBox(height: Gap.sm),
            Text(
              view.label,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.onVerdict,
                fontSize: 40,
                fontWeight: FontWeight.w800,
                letterSpacing: 1.5,
              ),
            ),
            Text(
              view.subtitle,
              style: TextStyle(
                color: AppColors.onVerdict.withValues(alpha: 0.92),
                fontSize: 17,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: Gap.md),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: Gap.md),
                child: _DetailCard(result: result, onDetails: onDetails),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(Gap.md),
              child: FilledButton.icon(
                onPressed: onNext,
                icon: const Icon(Icons.qr_code_scanner_rounded),
                label: const Text('Scanner suivant'),
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: view.color,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailCard extends StatelessWidget {
  final VerificationResult result;
  final VoidCallback? onDetails;
  const _DetailCard({required this.result, this.onDetails});

  @override
  Widget build(BuildContext context) {
    if (result.isNotFound) {
      return _wrap(child: _Line(label: 'Code', value: result.codebarre ?? '—'));
    }

    final uses = result.isUnlimited ? 'illimité' : '${result.usesSoFar} / ${result.maxAccess}';
    final f = result.flags;

    return _wrap(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          if (result.eventTitle != null)
            Text(result.eventTitle!,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          if (result.eventDate != null)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(Formatters.longDate(result.eventDate),
                  style: TextStyle(color: Colors.black.withValues(alpha: 0.6))),
            ),
          const SizedBox(height: Gap.sm),
          Wrap(
            spacing: Gap.sm,
            runSpacing: Gap.xs,
            children: [
              _Chip(result.type == 'BILLET' ? 'Billet' : 'Voucher'),
              if (result.ticketModel != null) _Chip(result.ticketModel!),
              ...result.accessZones.map(_Chip.new),
            ],
          ),
          const Divider(height: Gap.lg),
          StatusPillRow(
            vente: f.vendu,
            utilisation: f.utilisation,
            reservation: f.reservation,
            activation: f.activation,
            cancelled: f.cancelled,
          ),
          const SizedBox(height: Gap.md),
          _Line(label: 'Accès', value: uses),
          if (result.numeroserie != null) _Line(label: 'N° série', value: result.numeroserie!),
          _Line(label: 'Code à barre', value: result.codebarre ?? '—'),
          if (result.holderName != null) _Line(label: 'Titulaire', value: result.holderName!),
          if (result.affecteeA != null) _Line(label: 'Affecté à', value: result.affecteeA!),
          if (onDetails != null && result.numeroserie != null) ...[
            const SizedBox(height: Gap.sm),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: onDetails,
                icon: const Icon(Icons.info_outline_rounded),
                label: const Text('Plus de détails'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.primary,
                  side: const BorderSide(color: AppColors.primary),
                  minimumSize: const Size.fromHeight(48),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _wrap({required Widget child}) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Gap.md),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
        child: child,
      );
}

class _Line extends StatelessWidget {
  final String label;
  final String value;
  const _Line({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 104,
            child: Text(label,
                style: TextStyle(color: Colors.black.withValues(alpha: 0.55), fontSize: 14)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
          ),
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  final String label;
  const _Chip(this.label);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(label,
          style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w600, fontSize: 13)),
    );
  }
}

/// Shown for transport failures (timeout / offline / 401 / 5xx) — never a red
/// verdict, because "no answer" is not "invalid ticket".
class ScanErrorView extends StatelessWidget {
  final ApiException error;
  final VoidCallback onRetry;
  final VoidCallback onCancel;

  const ScanErrorView({
    super.key,
    required this.error,
    required this.onRetry,
    required this.onCancel,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF37474F),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Gap.lg),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.wifi_off_rounded, size: 96, color: Colors.white),
              const SizedBox(height: Gap.md),
              Text(error.frenchTitle,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white, fontSize: 30, fontWeight: FontWeight.w800)),
              const SizedBox(height: Gap.sm),
              Text(error.frenchHint,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.85), fontSize: 16)),
              const SizedBox(height: Gap.xl),
              FilledButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Réessayer'),
                style: FilledButton.styleFrom(
                    backgroundColor: Colors.white, foregroundColor: const Color(0xFF37474F)),
              ),
              const SizedBox(height: Gap.sm),
              TextButton(
                onPressed: onCancel,
                child: const Text('Annuler', style: TextStyle(color: Colors.white)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

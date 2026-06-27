import 'package:flutter/material.dart';

import '../../../../app/theme.dart';

/// Shown in "Info voucher" mode when the scanned code is NOT a voucher (a billet
/// or an unknown code). Info voucher queries the EXTERNAL commercial service,
/// which is meaningful only for vouchers — so we reject other types up front
/// with a clear, full-bleed message instead of a confusing empty result.
class WrongTypeView extends StatelessWidget {
  final VoidCallback onNext;
  const WrongTypeView({super.key, required this.onNext});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.verdictWarn,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Gap.lg),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.confirmation_number_outlined, size: 96, color: AppColors.onVerdict),
              const SizedBox(height: Gap.md),
              const Text(
                'VOUCHER UNIQUEMENT',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: AppColors.onVerdict,
                  fontSize: 32,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(height: Gap.sm),
              Text(
                "Le mode « Info voucher » n'accepte que les vouchers.\n"
                "Ce code n'est pas un voucher.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: AppColors.onVerdict.withValues(alpha: 0.92),
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: Gap.xl),
              FilledButton.icon(
                onPressed: onNext,
                icon: const Icon(Icons.qr_code_scanner_rounded),
                label: const Text('Scanner suivant'),
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: AppColors.verdictWarn,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
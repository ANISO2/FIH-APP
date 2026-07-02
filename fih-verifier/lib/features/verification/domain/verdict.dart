import 'package:flutter/material.dart';

import '../../../app/theme.dart';

/// The single clear outcome of a verification. Mirrors the backend enum
/// com.fih.companion.verification.Verdict exactly.
///
/// Note: CANCELLED is voucher-only. The backend's billet path hardcodes
/// cancelled = false, so a billet can only ever be valid / alreadyUsed /
/// notActive / notFound. We still handle all five generically.
enum Verdict {
  valid,
  alreadyUsed,
  notActive,
  cancelled,
  notFound;

  static Verdict fromApi(String? raw) {
    switch (raw) {
      case 'VALID':
        return Verdict.valid;
      case 'ALREADY_USED':
        return Verdict.alreadyUsed;
      case 'NOT_ACTIVE':
        return Verdict.notActive;
      case 'CANCELLED':
        return Verdict.cancelled;
      case 'NOT_FOUND':
      default:
        return Verdict.notFound;
    }
  }

  /// The only thing the operator must act on: green => let them through.
  bool get isPass => this == Verdict.valid;
}

/// How each verdict is shown on the full-bleed result screen.
class VerdictView {
  final Color color;
  final IconData icon;
  final String label;     // big word
  final String subtitle;  // one-line gate instruction

  const VerdictView(this.color, this.icon, this.label, this.subtitle);

  static VerdictView of(Verdict v) {
    switch (v) {
      case Verdict.valid:
        return const VerdictView(
            AppColors.verdictValid, Icons.check_circle_rounded,
            'VALIDE', 'Laisser passer');
      case Verdict.alreadyUsed:
        return const VerdictView(
            AppColors.verdictStop, Icons.history_rounded,
            'DÉJÀ UTILISÉ', 'Déjà passé — refuser');
      case Verdict.notActive:
        return const VerdictView(
            AppColors.verdictStop, Icons.block_rounded,
            'NON ACTIVÉ', 'Billet inactif — refuser');
      case Verdict.cancelled:
        return const VerdictView(
            AppColors.verdictStop, Icons.cancel_rounded,
            'ANNULÉ', 'Voucher annulé — refuser');
      case Verdict.notFound:
        return const VerdictView(
            AppColors.verdictStop, Icons.search_off_rounded,
            'INTROUVABLE', 'Code inconnu — refuser');
    }
  }
}
import 'package:flutter/material.dart';

import '../../../../app/theme.dart';

/// A labelled Oui/Non status pill, e.g.  Activation [Oui].
/// Matches the backoffice status badges in the mockups.
class StatusPill extends StatelessWidget {
  final String label;
  final bool value;

  /// When false, a `true` value is shown in a neutral/warning tone instead of
  /// green (e.g. "Utilisation = Oui" means already used — not a good thing).
  final bool positiveWhenTrue;

  const StatusPill({
    super.key,
    required this.label,
    required this.value,
    this.positiveWhenTrue = true,
  });

  @override
  Widget build(BuildContext context) {
    final bool good = value == positiveWhenTrue;
    final Color bg = good ? AppColors.verdictValid : const Color(0xFF9AA7B2);
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(fontSize: 12, color: Colors.black.withValues(alpha: 0.55))),
        const SizedBox(height: 3),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(20)),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(value ? Icons.check_rounded : Icons.close_rounded,
                  size: 14, color: Colors.white),
              const SizedBox(width: 4),
              Text(value ? 'Oui' : 'Non',
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 13)),
            ],
          ),
        ),
      ],
    );
  }
}

/// A wrap of the four ticket status pills used everywhere.
/// `vente`, `utilisation`, `reservation`, `activation` come from the scan flags.
class StatusPillRow extends StatelessWidget {
  final bool vente;
  final bool utilisation;
  final bool reservation;
  final bool activation;
  final bool? cancelled;

  const StatusPillRow({
    super.key,
    required this.vente,
    required this.utilisation,
    required this.reservation,
    required this.activation,
    this.cancelled,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 18,
      runSpacing: 12,
      children: [
        StatusPill(label: 'Activation', value: activation),
        // "used" being true is not a good thing at a gate, so don't paint it green.
        StatusPill(label: 'Utilisation', value: utilisation, positiveWhenTrue: false),
        StatusPill(label: 'Vente', value: vente),
        StatusPill(label: 'Réservation', value: reservation),
        if (cancelled == true)
          StatusPill(label: 'Annulé', value: true, positiveWhenTrue: false),
      ],
    );
  }
}

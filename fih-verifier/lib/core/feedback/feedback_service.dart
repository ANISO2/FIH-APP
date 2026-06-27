import 'package:flutter/services.dart';

import '../../features/verification/domain/verdict.dart';

/// Sound + haptic feedback for the gate loop.
///
/// Design intent: at a noisy turnstile in bright sun the operator should FEEL
/// and HEAR pass/no-pass without reading. Green = one clean cue; every stop
/// state = a heavier, more insistent cue.
///
/// Dependency-free on purpose for this phase: we use Flutter's built-in
/// [HapticFeedback] (rich, distinct impacts) and [SystemSound] (click / alert).
/// SystemSound has only two tones, so VALID uses `click` and all stop verdicts
/// use `alert`. For distinct multi-tone beeps per verdict we can add
/// `audioplayers` + five short assets in a later polish pass — isolated to this
/// one file, nothing else changes.
class FeedbackService {
  FeedbackService._();
  static final FeedbackService I = FeedbackService._();

  bool soundEnabled = true;
  bool hapticEnabled = true;

  Future<void> forVerdict(Verdict verdict) async {
    if (verdict == Verdict.valid) {
      await _valid();
    } else {
      await _stop();
    }
  }

  /// Distinct cue for a network failure (not a verdict): a light double-tap so
  /// the operator knows "no answer", not "rejected".
  Future<void> forError() async {
    if (hapticEnabled) {
      await HapticFeedback.lightImpact();
      await Future.delayed(const Duration(milliseconds: 90));
      await HapticFeedback.lightImpact();
    }
    if (soundEnabled) await SystemSound.play(SystemSoundType.click);
  }

  Future<void> _valid() async {
    if (hapticEnabled) await HapticFeedback.mediumImpact();
    if (soundEnabled) await SystemSound.play(SystemSoundType.click);
  }

  Future<void> _stop() async {
    if (hapticEnabled) {
      await HapticFeedback.heavyImpact();
      await Future.delayed(const Duration(milliseconds: 120));
      await HapticFeedback.heavyImpact();
    }
    if (soundEnabled) await SystemSound.play(SystemSoundType.alert);
  }
}

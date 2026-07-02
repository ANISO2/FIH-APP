import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/services.dart';

import '../../features/verification/domain/verdict.dart';

/// Sound + haptic feedback for the gate loop.
///
/// Design intent: at a noisy turnstile in bright sun the operator should FEEL
/// and HEAR pass/no-pass without reading. Green = one clean cue; every stop
/// state = a heavier, more insistent cue.
///
/// WHY THIS WAS SILENT BEFORE: the previous version used
/// `SystemSound.play(SystemSoundType.alert/click)`. On Android `alert` is a
/// no-op and `click` only plays when the device's *system touch sounds* are
/// enabled — so on a real phone there was effectively no audio, only haptics.
/// We now play bundled WAV assets through `audioplayers`, which always sounds
/// regardless of the touch-sound setting. Haptics are unchanged.
class FeedbackService {
  FeedbackService._();
  static final FeedbackService I = FeedbackService._();

  bool soundEnabled = true;
  bool hapticEnabled = true;

  // Low-latency mode is backed by SoundPool on Android — ideal for short,
  // repeatable gate cues. One reusable player is enough; a new play() retriggers
  // it. Created lazily-safe at construction.
  final AudioPlayer _player = AudioPlayer()
    ..setPlayerMode(PlayerMode.lowLatency)
    ..setReleaseMode(ReleaseMode.stop);

  // audioplayers' AssetSource prepends 'assets/', so these resolve to
  // assets/sounds/*.wav (declared in pubspec.yaml).
  static const String _acceptAsset = 'sounds/accept.wav';
  static const String _rejectAsset = 'sounds/reject.wav';

  Future<void> forVerdict(Verdict verdict) async {
    if (verdict == Verdict.valid) {
      await _valid();
    } else {
      await _stop();
    }
  }

  /// Distinct cue for a network failure (not a verdict): a light double-tap so
  /// the operator knows "no answer", not "rejected". Audibly reuses the reject
  /// tone; the double light haptic is what distinguishes it from a real refusal.
  Future<void> forError() async {
    if (hapticEnabled) {
      await HapticFeedback.lightImpact();
      await Future.delayed(const Duration(milliseconds: 90));
      await HapticFeedback.lightImpact();
    }
    await _play(_rejectAsset);
  }

  Future<void> _valid() async {
    if (hapticEnabled) await HapticFeedback.mediumImpact();
    await _play(_acceptAsset);
  }

  Future<void> _stop() async {
    if (hapticEnabled) {
      await HapticFeedback.heavyImpact();
      await Future.delayed(const Duration(milliseconds: 120));
      await HapticFeedback.heavyImpact();
    }
    await _play(_rejectAsset);
  }

  Future<void> _play(String asset) async {
    if (!soundEnabled) return;
    try {
      // stop() first so a rapid second scan retriggers cleanly instead of being
      // dropped while the previous cue is still playing.
      await _player.stop();
      await _player.play(AssetSource(asset), volume: 1.0);
    } catch (_) {
      // Audio must never break the scan loop; fall back to the system click.
      await SystemSound.play(SystemSoundType.click);
    }
  }
}

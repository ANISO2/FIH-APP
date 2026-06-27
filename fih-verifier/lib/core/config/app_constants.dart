/// Static, app-wide constants. No feature logic here.
class AppConstants {
  AppConstants._();

  static const String appName = 'FIH Verifier';

  /// How long the verdict stays on screen before the camera is hot again if the
  /// operator doesn't tap "Scanner suivant". 0 = stay until tapped.
  static const Duration autoResetAfterResult = Duration.zero;

  /// Live-stats poll interval. Kept gentle: several of the polled endpoints are
  /// live (uncached) reads on a SHARED database, so we stay a good citizen.
  static const Duration statsPollInterval = Duration(seconds: 25);
}

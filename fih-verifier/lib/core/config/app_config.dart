import 'package:shared_preferences/shared_preferences.dart';

/// Runtime configuration.
///
/// The backend API base URL is now CHANGEABLE AT RUNTIME (Réglages → API) and
/// PERSISTED on the device, so the same APK can be pointed at another similar
/// backend without rebuilding. The compile-time `--dart-define=FIH_API=...`
/// value is used only as the initial seed / factory default.
///
/// Example seed (replace the IP with your PC's LAN address on the same Wi-Fi):
///   flutter run --dart-define=FIH_API=http://192.168.1.10:8080 \
///               --dart-define=FIH_DEVICE_TOKEN=dev-device-token
///
/// Default 10.0.2.2 is the Android emulator's alias for the host's localhost.
///
/// NO LOGIN: the operator never types credentials. The app authenticates with a
/// single shared device token sent in the X-Device-Token header on every
/// request (see DioClient). The backend's DeviceTokenFilter maps that header to
/// ROLE_DEVICE, allowed on /api/verify/** and the global stats reads.
class AppConfig {
  AppConfig._();

  // --- Compile-time seeds (still injectable with --dart-define) ------------
  static const String _envApiBaseUrl =
      String.fromEnvironment('FIH_API', defaultValue: 'http://10.0.2.2:8080');

  static const String deviceToken =
      String.fromEnvironment('FIH_DEVICE_TOKEN', defaultValue: 'dev-device-token');

  static const String _prefsKeyApiBaseUrl = 'fih_api_base_url';

  /// The live base URL. Read fresh by [ApiEndpoints] on every request, so a
  /// change made in Réglages takes effect immediately for new requests — no app
  /// restart needed. Seeded from the --dart-define default, then overridden by
  /// the persisted value (if any) in [load].
  static String apiBaseUrl = _normalize(_envApiBaseUrl);

  /// The factory default (the --dart-define value), exposed for a "reset"
  /// affordance in settings.
  static String get defaultApiBaseUrl => _normalize(_envApiBaseUrl);

  /// Load the persisted base URL. Call ONCE at startup, before runApp, so the
  /// very first request already targets the chosen backend.
  static Future<void> load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final saved = prefs.getString(_prefsKeyApiBaseUrl);
      if (saved != null && saved.trim().isNotEmpty) {
        apiBaseUrl = _normalize(saved);
      }
    } catch (_) {
      // If prefs are unavailable for any reason, keep the compile-time seed.
    }
  }

  /// Persist + apply a new base URL at runtime. Applied in-memory immediately
  /// (so the next Dio request uses it) and saved for next launch.
  static Future<void> setApiBaseUrl(String url) async {
    final cleaned = _normalize(url);
    apiBaseUrl = cleaned;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_prefsKeyApiBaseUrl, cleaned);
    } catch (_) {
      // In-memory value still applies even if persistence fails.
    }
  }

  /// Trim whitespace and trailing slashes so endpoint building ("$base/api/...")
  /// never produces a double slash.
  static String _normalize(String url) {
    var u = url.trim();
    while (u.endsWith('/')) {
      u = u.substring(0, u.length - 1);
    }
    return u;
  }
}

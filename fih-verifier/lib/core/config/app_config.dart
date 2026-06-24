/// Runtime configuration, injected at run/build time with --dart-define.
///
/// Example (replace the IP with your PC's LAN address on the same Wi-Fi):
///   flutter run --dart-define=FIH_API=http://192.168.1.10:8080
///
/// Default 10.0.2.2 is the Android emulator's alias for the host machine's
/// localhost. Nothing consumes this yet — it is wired up in the next phase.
class AppConfig {
  AppConfig._();

  static const String apiBaseUrl =
      String.fromEnvironment('FIH_API', defaultValue: 'http://10.0.2.2:8080');
}

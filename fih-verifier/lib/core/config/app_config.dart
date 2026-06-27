/// Runtime configuration, injected at run/build time with --dart-define.
///
/// Example (replace the IP with your PC's LAN address on the same Wi-Fi):
///   flutter run --dart-define=FIH_API=http://192.168.1.10:8080 \
///               --dart-define=FIH_DEVICE_TOKEN=dev-device-token
///
/// Default 10.0.2.2 is the Android emulator's alias for the host machine's
/// localhost.
///
/// NO LOGIN: the operator never types credentials. The app authenticates with a
/// single shared device token sent in the X-Device-Token header on every
/// request (see DioClient). The backend's DeviceTokenFilter maps that header to
/// ROLE_DEVICE, which is allowed on /api/verify/** and the global stats reads.
/// The default below matches the backend's local default
/// (fih.security.device-token = dev-device-token). For a real deployment, set
/// the SAME secret on the server (env FIH_DEVICE_TOKEN) and in the app build
/// (--dart-define=FIH_DEVICE_TOKEN=...).
class AppConfig {
  AppConfig._();

  static const String apiBaseUrl =
      String.fromEnvironment('FIH_API', defaultValue: 'http://10.0.2.2:8080');

  static const String deviceToken =
      String.fromEnvironment('FIH_DEVICE_TOKEN', defaultValue: 'dev-device-token');
}

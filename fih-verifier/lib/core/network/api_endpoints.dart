import '../config/app_config.dart';

/// Endpoint paths exposed by fih-companion-api. Declared for the next phase;
/// no HTTP client is implemented in the base.
///   GET /api/verify/billet/{code}
///   GET /api/verify/voucher/{code}
class ApiEndpoints {
  ApiEndpoints._();

  static String verifyBillet(String code) =>
      '${AppConfig.apiBaseUrl}/api/verify/billet/$code';

  static String verifyVoucher(String code) =>
      '${AppConfig.apiBaseUrl}/api/verify/voucher/$code';
}

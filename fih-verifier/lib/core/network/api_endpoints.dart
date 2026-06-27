import '../config/app_config.dart';

/// Endpoint paths exposed by fih-companion-api.
///
/// The scanned code is sent as a QUERY PARAMETER (?code=...) by the data source,
/// NOT embedded in the path — a QR/barcode can contain slashes, newlines or a
/// whole URL, which would otherwise break the request (servlet firewall →
/// "erreur serveur"). So the verify / voucher-info builders take no argument.
///
/// Verification (always HTTP 200, even NOT_FOUND):
///   GET /api/verify/billet?code=...        -> VerificationResult  (Feature 2)
///   GET /api/verify/voucher?code=...       -> VerificationResult  (Feature 2)
///   GET /api/verify/voucher-info?code=...  -> VoucherInfoResponse  (Feature 1, external)
///
/// Lazy details, keyed by numéro de série (clean DB value, safe in the path):
///   GET /api/verify/billet/{numeroserie}/details
///   GET /api/verify/voucher/{numeroserie}/details
class ApiEndpoints {
  ApiEndpoints._();

  static String get _base => AppConfig.apiBaseUrl;

  // ---- Verification (Feature 2: our DB) — code goes in queryParameters ----
  static String verifyBillet() => '$_base/api/verify/billet';
  static String verifyVoucher() => '$_base/api/verify/voucher';

  // ---- Voucher info (Feature 1: external service) — code goes in queryParameters ----
  static String voucherInfo() => '$_base/api/verify/voucher-info';

  // ---- Lazy details (access log + management extras) — numeroserie in path ----
  static String billetDetails(String numeroserie) =>
      '$_base/api/verify/billet/${_enc(numeroserie)}/details';
  static String voucherDetails(String numeroserie) =>
      '$_base/api/verify/voucher/${_enc(numeroserie)}/details';

  // ---- Global stats (Feature 3) ----
  static String statsOverview() => '$_base/api/stats/overview';
  static String statsYears() => '$_base/api/stats/years';
  static String statsEntriesByDay() => '$_base/api/stats/entries-by-day';
  static String statsGate() => '$_base/api/stats/gate';
  static String statsTicketTypes() => '$_base/api/stats/ticket-types';
  static String statsTourniquets() => '$_base/api/stats/tourniquets';
  static String statsRejets() => '$_base/api/stats/rejets';

  static String _enc(String value) => Uri.encodeComponent(value.trim());
}
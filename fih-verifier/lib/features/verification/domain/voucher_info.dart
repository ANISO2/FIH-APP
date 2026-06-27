import '../../../core/utils/formatters.dart';

/// Status of the external voucher-info lookup (Feature 1).
enum VoucherInfoStatus {
  ok,
  notFound,
  pendingIntegration;

  static VoucherInfoStatus fromApi(String? raw) {
    switch (raw) {
      case 'OK':
        return VoucherInfoStatus.ok;
      case 'NOT_FOUND':
        return VoucherInfoStatus.notFound;
      case 'PENDING_INTEGRATION':
      default:
        return VoucherInfoStatus.pendingIntegration;
    }
  }
}

/// Mirrors the backend record VoucherInfoResponse. The contract is FROZEN; only
/// the implementation behind VoucherVerificationGateway changes later. Today the
/// stub returns PENDING_INTEGRATION with the fields null.
class VoucherInfo {
  final VoucherInfoStatus status;
  final String? source; // always "EXTERNAL_SERVICE"
  final String? code;
  final String? numeroserie;
  final String? codebarre;
  final String? eventTitle;
  final DateTime? eventDate;
  final String? model;
  final double? prix;
  final bool? vendu;
  final DateTime? dateVente;
  final int? accessCounter;
  final String? message;

  const VoucherInfo({
    required this.status,
    required this.source,
    required this.code,
    required this.numeroserie,
    required this.codebarre,
    required this.eventTitle,
    required this.eventDate,
    required this.model,
    required this.prix,
    required this.vendu,
    required this.dateVente,
    required this.accessCounter,
    required this.message,
  });

  bool get isPending => status == VoucherInfoStatus.pendingIntegration;
  bool get isNotFound => status == VoucherInfoStatus.notFound;

  factory VoucherInfo.fromJson(Map<String, dynamic> j) {
    return VoucherInfo(
      status: VoucherInfoStatus.fromApi(j['status'] as String?),
      source: j['source'] as String?,
      code: j['code'] as String?,
      numeroserie: j['numeroserie'] as String?,
      codebarre: j['codebarre'] as String?,
      eventTitle: j['eventTitle'] as String?,
      eventDate: Formatters.parseDate(j['eventDate']),
      model: j['model'] as String?,
      prix: (j['prix'] as num?)?.toDouble(),
      vendu: j['vendu'] as bool?,
      dateVente: Formatters.parseDate(j['dateVente']),
      accessCounter: (j['accessCounter'] as num?)?.toInt(),
      message: j['message'] as String?,
    );
  }
}

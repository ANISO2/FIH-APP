import '../../../core/utils/formatters.dart';
import 'verdict.dart';

/// Mirrors the backend record com.fih.companion.verification.VerificationResult.
///
/// CONTRACT NOTE (these tripped up the brief — get them right):
///   JSON keys are `ticketModel`, `accessZones`, `usesSoFar`
///   (NOT modelName / zones / uses).
class VerificationFlags {
  final bool activation;
  final bool utilisation;
  final bool vendu;
  final bool reservation;
  final bool cancelled;

  const VerificationFlags({
    required this.activation,
    required this.utilisation,
    required this.vendu,
    required this.reservation,
    required this.cancelled,
  });

  factory VerificationFlags.fromJson(Map<String, dynamic>? j) {
    j ??= const {};
    bool b(String k) => j![k] == true;
    return VerificationFlags(
      activation: b('activation'),
      utilisation: b('utilisation'),
      vendu: b('vendu'),
      reservation: b('reservation'),
      cancelled: b('cancelled'),
    );
  }

  static const empty =
      VerificationFlags(activation: false, utilisation: false, vendu: false, reservation: false, cancelled: false);
}

class VerificationResult {
  final String type; // "BILLET" | "VOUCHER"
  final Verdict verdict;
  final String? numeroserie;
  final String? codebarre;
  final String? eventTitle;
  final DateTime? eventDate;
  final String? ticketModel;
  final List<String> accessZones;
  final int maxAccess; // 0 = unlimited
  final int usesSoFar;
  final String? holderName; // billet only
  final String? affecteeA; // from app-owned badge_affectation, or null
  final VerificationFlags flags;

  const VerificationResult({
    required this.type,
    required this.verdict,
    required this.numeroserie,
    required this.codebarre,
    required this.eventTitle,
    required this.eventDate,
    required this.ticketModel,
    required this.accessZones,
    required this.maxAccess,
    required this.usesSoFar,
    required this.holderName,
    required this.affecteeA,
    required this.flags,
  });

  bool get isUnlimited => maxAccess == 0;
  bool get isNotFound => verdict == Verdict.notFound;

  factory VerificationResult.fromJson(Map<String, dynamic> j) {
    return VerificationResult(
      type: (j['type'] as String?) ?? '',
      verdict: Verdict.fromApi(j['verdict'] as String?),
      numeroserie: j['numeroserie'] as String?,
      codebarre: j['codebarre'] as String?,
      eventTitle: j['eventTitle'] as String?,
      eventDate: Formatters.parseDate(j['eventDate']),
      ticketModel: j['ticketModel'] as String?,
      accessZones: (j['accessZones'] as List?)?.map((e) => e.toString()).toList() ?? const [],
      maxAccess: (j['maxAccess'] as num?)?.toInt() ?? 0,
      usesSoFar: (j['usesSoFar'] as num?)?.toInt() ?? 0,
      holderName: j['holderName'] as String?,
      affecteeA: j['affecteeA'] as String?,
      flags: VerificationFlags.fromJson(j['flags'] as Map<String, dynamic>?),
    );
  }
}

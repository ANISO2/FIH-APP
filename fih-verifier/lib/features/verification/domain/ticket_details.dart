import '../../../core/utils/formatters.dart';
import 'access_log.dart';

/// Lazy-loaded extras + access history for the ℹ screen. Mirrors the backend
/// TicketDetailsResponse. Billet fills livre/dateLivraison/dateVente; voucher
/// fills commande/dateVente.
class TicketDetails {
  final String type; // BILLET | VOUCHER
  final String? numeroserie;
  final String? codebarre;
  final bool? livre;
  final DateTime? dateLivraison;
  final DateTime? dateVente;
  final int? commande;
  final List<AccessLogEntry> accessPublic;
  final List<AccessLogEntry> accessVip;

  const TicketDetails({
    required this.type,
    required this.numeroserie,
    required this.codebarre,
    required this.livre,
    required this.dateLivraison,
    required this.dateVente,
    required this.commande,
    required this.accessPublic,
    required this.accessVip,
  });

  int get totalScans => accessPublic.length + accessVip.length;

  factory TicketDetails.fromJson(Map<String, dynamic> j) {
    List<AccessLogEntry> list(Object? raw) => (raw as List?)
            ?.map((e) => AccessLogEntry.fromJson(e as Map<String, dynamic>))
            .toList() ??
        const [];
    return TicketDetails(
      type: (j['type'] as String?) ?? '',
      numeroserie: j['numeroserie'] as String?,
      codebarre: j['codebarre'] as String?,
      livre: j['livre'] as bool?,
      dateLivraison: Formatters.parseDate(j['dateLivraison']),
      dateVente: Formatters.parseDate(j['dateVente']),
      commande: (j['commande'] as num?)?.toInt(),
      accessPublic: list(j['accessPublic']),
      accessVip: list(j['accessVip']),
    );
  }
}

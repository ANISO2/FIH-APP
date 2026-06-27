import '../../../core/utils/formatters.dart';

/// One turnstile access-log line (from tturnstile = Public, vipaccess = VIP).
class AccessLogEntry {
  final int? reference;
  final String? codebarre;
  final DateTime? date;
  final DateTime? time;
  final String? porte;
  final bool granted; // transactionstate: true = autorisé (vert), false = refusé (rouge)

  const AccessLogEntry({
    required this.reference,
    required this.codebarre,
    required this.date,
    required this.time,
    required this.porte,
    required this.granted,
  });

  factory AccessLogEntry.fromJson(Map<String, dynamic> j) => AccessLogEntry(
        reference: (j['reference'] as num?)?.toInt(),
        codebarre: j['codebarre'] as String?,
        date: Formatters.parseDate(j['date']),
        time: Formatters.parseDate(j['time']),
        porte: j['porte'] as String?,
        granted: j['granted'] == true,
      );
}

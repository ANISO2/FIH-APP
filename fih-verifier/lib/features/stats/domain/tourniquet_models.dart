import '../../../core/utils/formatters.dart';

/// Models for the backoffice "Statistique des tourniquets" view, consumed by
/// the mobile "today details" screen. These mirror the existing backend records
/// TourniquetEventDto / TourniquetRowDto (read-only, device-token allowed via
/// GET /api/stats/tourniquets). No new backend endpoint is needed.

/// One model row inside an event.
///   Code à barre accessibles : billetCodes / voucherCodes / audience
///   Transactions tourniquet  : billetTransactions / voucherTransactions
class TourniquetRow {
  final int modelId;
  final String modelName;
  final int billetCodes;
  final int voucherCodes;
  final int audience;
  final int billetTransactions;
  final int voucherTransactions;

  const TourniquetRow({
    required this.modelId,
    required this.modelName,
    required this.billetCodes,
    required this.voucherCodes,
    required this.audience,
    required this.billetTransactions,
    required this.voucherTransactions,
  });

  factory TourniquetRow.fromJson(Map<String, dynamic> j) => TourniquetRow(
        modelId: _i(j['modelId']),
        modelName: (j['modelName'] as String?) ?? '—',
        billetCodes: _i(j['billetCodes']),
        voucherCodes: _i(j['voucherCodes']),
        audience: _i(j['audience']),
        billetTransactions: _i(j['billetTransactions']),
        voucherTransactions: _i(j['voucherTransactions']),
      );
}

/// One event header + its model rows.
///   audience            -> "Émis"
///   transactionsBillets -> "Transactions Billets"
///   transactionsVouchers-> "Transactions Vouchers"
///   tourniquets         -> "Entrées"
///   presence (computed) -> "Présence"
class TourniquetEvent {
  final int eventId;
  final String eventTitle;
  final DateTime? eventDate;
  final int audience;
  final int transactionsBillets;
  final int transactionsVouchers;
  final int tourniquets;
  final List<TourniquetRow> rows;

  const TourniquetEvent({
    required this.eventId,
    required this.eventTitle,
    required this.eventDate,
    required this.audience,
    required this.transactionsBillets,
    required this.transactionsVouchers,
    required this.tourniquets,
    required this.rows,
  });

  /// Présence = entrées / émis, 0..100.
  double get presence => audience == 0 ? 0 : (tourniquets / audience) * 100.0;

  factory TourniquetEvent.fromJson(Map<String, dynamic> j) => TourniquetEvent(
        eventId: _i(j['eventId']),
        eventTitle: (j['eventTitle'] as String?) ?? '—',
        eventDate: Formatters.parseDate(j['eventDate']),
        audience: _i(j['audience']),
        transactionsBillets: _i(j['transactionsBillets']),
        transactionsVouchers: _i(j['transactionsVouchers']),
        tourniquets: _i(j['tourniquets']),
        rows: ((j['rows'] as List<dynamic>?) ?? const [])
            .map((e) => TourniquetRow.fromJson(e as Map<String, dynamic>))
            .toList(growable: false),
      );
}

int _i(Object? v) => (v as num?)?.toInt() ?? 0;

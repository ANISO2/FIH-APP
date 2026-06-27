import '../../../core/utils/formatters.dart';

/// Models for the GLOBAL, non-sensitive stats the mobile dashboard reads.
/// No money / recette here — those endpoints are ADMIN-only and intentionally
/// excluded. Every field is a server-side aggregate (counts), so payloads stay
/// tiny even when the turnstile log holds tens of thousands of scans.

class StatsOverview {
  final int totalEvents;
  final int totalBillets;
  final int totalVouchers;
  final int totalScans;
  final int acceptedScans; // "entrées" — people who got through
  final int rejectedScans;
  final double acceptanceRate; // 0..100
  final int publicScans;
  final int vipScans;
  final String? busiestEventTitle;
  final DateTime? busiestEventDate;
  final int busiestEventScans;

  const StatsOverview({
    required this.totalEvents,
    required this.totalBillets,
    required this.totalVouchers,
    required this.totalScans,
    required this.acceptedScans,
    required this.rejectedScans,
    required this.acceptanceRate,
    required this.publicScans,
    required this.vipScans,
    required this.busiestEventTitle,
    required this.busiestEventDate,
    required this.busiestEventScans,
  });

  factory StatsOverview.fromJson(Map<String, dynamic> j) => StatsOverview(
        totalEvents: _i(j['totalEvents']),
        totalBillets: _i(j['totalBillets']),
        totalVouchers: _i(j['totalVouchers']),
        totalScans: _i(j['totalScans']),
        acceptedScans: _i(j['acceptedScans']),
        rejectedScans: _i(j['rejectedScans']),
        acceptanceRate: (j['acceptanceRate'] as num?)?.toDouble() ?? 0,
        publicScans: _i(j['publicScans']),
        vipScans: _i(j['vipScans']),
        busiestEventTitle: j['busiestEventTitle'] as String?,
        busiestEventDate: Formatters.parseDate(j['busiestEventDate']),
        busiestEventScans: _i(j['busiestEventScans']),
      );
}

class GateBucket {
  final int scans;
  final int accepted;
  final int rejected;
  const GateBucket({required this.scans, required this.accepted, required this.rejected});

  factory GateBucket.fromJson(Map<String, dynamic>? j) => GateBucket(
        scans: _i(j?['scans']),
        accepted: _i(j?['accepted']),
        rejected: _i(j?['rejected']),
      );
}

class GateStats {
  final GateBucket publicGate;
  final GateBucket vip;
  const GateStats({required this.publicGate, required this.vip});

  factory GateStats.fromJson(Map<String, dynamic> j) => GateStats(
        publicGate: GateBucket.fromJson(j['public'] as Map<String, dynamic>?),
        vip: GateBucket.fromJson(j['vip'] as Map<String, dynamic>?),
      );
}

class TicketBucket {
  final int issued;
  final int scanned;
  const TicketBucket({required this.issued, required this.scanned});

  factory TicketBucket.fromJson(Map<String, dynamic>? j) =>
      TicketBucket(issued: _i(j?['issued']), scanned: _i(j?['scanned']));
}

class TicketTypes {
  final TicketBucket billet;
  final TicketBucket voucher;
  const TicketTypes({required this.billet, required this.voucher});

  factory TicketTypes.fromJson(Map<String, dynamic> j) => TicketTypes(
        billet: TicketBucket.fromJson(j['billet'] as Map<String, dynamic>?),
        voucher: TicketBucket.fromJson(j['voucher'] as Map<String, dynamic>?),
      );
}

class EntryByDay {
  final DateTime? date;
  final int scans;
  final int accepted;
  final int rejected;
  const EntryByDay({
    required this.date,
    required this.scans,
    required this.accepted,
    required this.rejected,
  });

  factory EntryByDay.fromJson(Map<String, dynamic> j) => EntryByDay(
        date: Formatters.parseDate(j['date']),
        scans: _i(j['scans']),
        accepted: _i(j['accepted']),
        rejected: _i(j['rejected']),
      );
}

/// One snapshot of the whole dashboard, fetched together.
class StatsDashboard {
  final StatsOverview overview;
  final GateStats gate;
  final TicketTypes ticketTypes;
  final List<EntryByDay> entriesByDay;

  const StatsDashboard({
    required this.overview,
    required this.gate,
    required this.ticketTypes,
    required this.entriesByDay,
  });
}

int _i(Object? v) => (v as num?)?.toInt() ?? 0;

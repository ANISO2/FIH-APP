import '../domain/stats_models.dart';
import '../domain/tourniquet_models.dart';
import 'stats_remote_data_source.dart';

/// Orchestrates the dashboard loads. The "today" view needs only the
/// entries-by-day feed (small, cache-friendly), which we poll live.
class StatsRepository {
  final StatsRemoteDataSource _remote;
  StatsRepository(this._remote);

  Future<List<int>> years() => _remote.years();

  /// Per-day turnstile entries (date / scans / accepted / rejected). The today
  /// view picks today's row from this list.
  Future<List<EntryByDay>> entriesByDay(int? year) => _remote.entriesByDay(year);

  /// Backoffice "Statistique des tourniquets" feed for the today-details screen.
  Future<List<TourniquetEvent>> tourniquets(int? year) => _remote.tourniquets(year);

  /// Full global snapshot (kept for reuse; not used by the today view).
  Future<StatsDashboard> dashboard(int? year) async {
    final results = await Future.wait<Object>([
      _remote.overview(year),
      _remote.gate(year),
      _remote.ticketTypes(year),
      _remote.entriesByDay(year),
    ]);
    return StatsDashboard(
      overview: results[0] as StatsOverview,
      gate: results[1] as GateStats,
      ticketTypes: results[2] as TicketTypes,
      entriesByDay: results[3] as List<EntryByDay>,
    );
  }
}

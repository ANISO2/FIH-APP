import '../domain/stats_models.dart';
import 'stats_remote_data_source.dart';

/// Orchestrates the dashboard load. The four global feeds are fetched in
/// parallel into a single snapshot so one refresh = one short burst of small,
/// cache-friendly reads.
class StatsRepository {
  final StatsRemoteDataSource _remote;
  StatsRepository(this._remote);

  Future<List<int>> years() => _remote.years();

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

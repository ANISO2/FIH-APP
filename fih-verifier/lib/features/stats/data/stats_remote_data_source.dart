import 'package:dio/dio.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../domain/stats_models.dart';
import '../domain/tourniquet_models.dart';

/// Reads the GLOBAL stats feeds (device-token allowed). Year is sent as an
/// optional `?year=` query param. Polls hit the server's short-TTL cache (we
/// never force a cache bypass), so repeated reads under a busy gate stay cheap.
class StatsRemoteDataSource {
  final Dio _dio;
  StatsRemoteDataSource(this._dio);

  Future<List<int>> years() async {
    try {
      final res = await _dio.get<List<dynamic>>(ApiEndpoints.statsYears());
      return (res.data ?? const [])
          .map((e) => (e as num).toInt())
          .toList(growable: false);
    } catch (e) {
      throw DioClient.mapError(e);
    }
  }

  Future<StatsOverview> overview(int? year) =>
      _obj(ApiEndpoints.statsOverview(), year, StatsOverview.fromJson);

  Future<GateStats> gate(int? year) =>
      _obj(ApiEndpoints.statsGate(), year, GateStats.fromJson);

  Future<TicketTypes> ticketTypes(int? year) =>
      _obj(ApiEndpoints.statsTicketTypes(), year, TicketTypes.fromJson);

  Future<List<EntryByDay>> entriesByDay(int? year) =>
      _list(ApiEndpoints.statsEntriesByDay(), year, EntryByDay.fromJson);

  /// Backoffice "Statistique des tourniquets" feed (per event + per model). Used
  /// by the mobile "today details" screen. Already device-readable — no new
  /// backend endpoint.
  Future<List<TourniquetEvent>> tourniquets(int? year) =>
      _list(ApiEndpoints.statsTourniquets(), year, TourniquetEvent.fromJson);

  Future<T> _obj<T>(String url, int? year, T Function(Map<String, dynamic>) parse) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>(url, queryParameters: _q(year));
      final data = res.data;
      if (data == null) throw const ApiException(ApiErrorKind.decode, 'Empty body');
      return parse(data);
    } catch (e) {
      throw DioClient.mapError(e);
    }
  }

  Future<List<T>> _list<T>(String url, int? year, T Function(Map<String, dynamic>) parse) async {
    try {
      final res = await _dio.get<List<dynamic>>(url, queryParameters: _q(year));
      return (res.data ?? const [])
          .map((e) => parse(e as Map<String, dynamic>))
          .toList(growable: false);
    } catch (e) {
      throw DioClient.mapError(e);
    }
  }

  Map<String, dynamic>? _q(int? year) => year == null ? null : {'year': year};
}

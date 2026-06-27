import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../../core/network/api_exception.dart';
import '../../data/stats_repository.dart';
import '../../domain/stats_models.dart';

sealed class StatsUiState {
  const StatsUiState();
}

class StatsLoading extends StatsUiState {
  const StatsLoading();
}

class StatsLoaded extends StatsUiState {
  final StatsDashboard data;
  const StatsLoaded(this.data);
}

class StatsFailed extends StatsUiState {
  final String message;
  const StatsFailed(this.message);
}

/// Drives the dashboard.
///
/// - Auto-refresh is OFF by default; the user turns it on with a switch. When
///   on, it polls every [pollInterval] (30 s) — granular enough for a gate that
///   sees 10k+ scans in half an hour, light enough to ride the server cache.
/// - Polls are non-overlapping (`_inFlight`) and OFFLINE-TOLERANT: a failed
///   refresh keeps the last good data on screen and just raises [offlineHint],
///   instead of throwing the dashboard away.
class StatsController extends ChangeNotifier {
  final StatsRepository _repo;
  StatsController(this._repo);

  static const Duration pollInterval = Duration(seconds: 30);

  StatsUiState _state = const StatsLoading();
  StatsUiState get state => _state;

  List<int> years = const [];
  int? year; // null = "Toutes les années"
  bool autoRefresh = false;
  bool offlineHint = false; // last refresh failed but we kept cached data
  DateTime? lastUpdated;

  bool _inFlight = false;
  Timer? _timer;

  Future<void> init() async {
    try {
      years = await _repo.years();
      if (years.isNotEmpty) year = years.first; // default to the most recent year
    } catch (_) {
      // Year list is non-critical; the dashboard still works for "all years".
    }
    await refresh();
  }

  Future<void> setYear(int? value) async {
    if (value == year) return;
    year = value;
    _state = const StatsLoading();
    notifyListeners();
    await refresh();
  }

  Future<void> refresh() async {
    if (_inFlight) return;
    _inFlight = true;
    try {
      final data = await _repo.dashboard(year);
      _state = StatsLoaded(data);
      lastUpdated = DateTime.now();
      offlineHint = false;
    } on ApiException catch (e) {
      if (_state is StatsLoaded) {
        offlineHint = true; // keep showing the last snapshot
      } else {
        _state = StatsFailed(e.frenchHint);
      }
    } catch (_) {
      if (_state is StatsLoaded) {
        offlineHint = true;
      } else {
        _state = const StatsFailed('Impossible de charger les statistiques.');
      }
    } finally {
      _inFlight = false;
      notifyListeners();
    }
  }

  void setAutoRefresh(bool on) {
    autoRefresh = on;
    _timer?.cancel();
    _timer = null;
    if (on) {
      _timer = Timer.periodic(pollInterval, (_) => refresh());
    }
    notifyListeners();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

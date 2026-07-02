import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../../core/network/api_exception.dart';
import '../../data/stats_repository.dart';
import '../../domain/stats_models.dart';

/// Today's numbers, derived from the per-day entries feed.
class TodayStats {
  final DateTime day;   // the day actually shown
  final bool isToday;   // false when we fell back to the latest active day
  final int entries;    // accepted passages
  final int rejected;
  final int total;      // all scans that day
  final double rate;    // acceptance %, 0..100

  const TodayStats({
    required this.day,
    required this.isToday,
    required this.entries,
    required this.rejected,
    required this.total,
    required this.rate,
  });
}

sealed class StatsUiState {
  const StatsUiState();
}

class StatsLoading extends StatsUiState {
  const StatsLoading();
}

class StatsLoaded extends StatsUiState {
  final TodayStats today;
  const StatsLoaded(this.today);
}

class StatsFailed extends StatsUiState {
  final String message;
  const StatsFailed(this.message);
}

/// Drives the TODAY dashboard.
///
/// - LIVE by default: polls every [pollInterval] so the numbers update on their
///   own while the gates are open.
/// - FREEZE button: [setLive(false)] stops the polling and holds the current
///   snapshot on screen (it stays frozen until you resume or restart the app).
///   [setLive(true)] resumes the live feed.
/// - Polls are non-overlapping and offline-tolerant: a failed poll keeps the
///   last snapshot and just raises [offlineHint].
class StatsController extends ChangeNotifier {
  final StatsRepository _repo;
  StatsController(this._repo);

  static const Duration pollInterval = Duration(seconds: 15);

  StatsUiState _state = const StatsLoading();
  StatsUiState get state => _state;

  bool live = true; // live updating ON by default
  bool offlineHint = false;
  DateTime? lastUpdated;

  bool _inFlight = false;
  Timer? _timer;

  Future<void> init() async {
    await refresh();
    if (live) _startTimer();
  }

  void _startTimer() {
    _timer?.cancel();
    _timer = Timer.periodic(pollInterval, (_) => refresh());
  }

  Future<void> refresh() async {
    if (_inFlight) return;
    _inFlight = true;
    try {
      final days = await _repo.entriesByDay(null);
      _state = StatsLoaded(_buildToday(days));
      lastUpdated = DateTime.now();
      offlineHint = false;
    } on ApiException catch (e) {
      if (_state is StatsLoaded) {
        offlineHint = true;
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

  /// Freeze (false) / resume live (true).
  void setLive(bool on) {
    live = on;
    if (on) {
      _startTimer();
      refresh();
    } else {
      _timer?.cancel();
      _timer = null;
    }
    notifyListeners();
  }

  TodayStats _buildToday(List<EntryByDay> days) {
    final now = DateTime.now();
    bool isSameDay(DateTime d) => d.year == now.year && d.month == now.month && d.day == now.day;

    final sorted = days.where((d) => d.date != null).toList()
      ..sort((a, b) => a.date!.compareTo(b.date!));

    EntryByDay? todayRow;
    for (final d in sorted) {
      if (isSameDay(d.date!)) {
        todayRow = d;
        break;
      }
    }

    final chosen = todayRow ?? (sorted.isNotEmpty ? sorted.last : null);

    if (chosen == null) {
      return TodayStats(
        day: DateTime(now.year, now.month, now.day),
        isToday: true,
        entries: 0,
        rejected: 0,
        total: 0,
        rate: 0,
      );
    }

    final total = chosen.scans;
    final rate = total == 0 ? 0.0 : (chosen.accepted / total) * 100.0;
    return TodayStats(
      day: chosen.date!,
      isToday: todayRow != null,
      entries: chosen.accepted,
      rejected: chosen.rejected,
      total: total,
      rate: rate,
    );
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

import 'package:flutter/foundation.dart';

import '../../../../core/feedback/feedback_service.dart';
import '../../../../core/network/api_exception.dart';
import '../../data/verification_repository.dart';
import '../../domain/scan_mode.dart';
import '../../domain/verdict.dart';
import '../../domain/verification_result.dart';
import '../../domain/voucher_info.dart';

/// The scanner's screen state. Plain sealed-style hierarchy so the UI can switch
/// on it. Built-in ChangeNotifier — no state-management package needed.
sealed class ScanState {
  const ScanState();
}

/// Camera live, waiting for a code.
class ScanIdle extends ScanState {
  const ScanIdle();
}

/// A code was read; a request is in flight.
class ScanLoading extends ScanState {
  final String code;
  const ScanLoading(this.code);
}

/// Feature 2 — a verdict came back.
class ScanResult extends ScanState {
  final VerificationResult result;
  const ScanResult(this.result);
}

/// Feature 1 — voucher info came back.
class ScanInfo extends ScanState {
  final VoucherInfo info;
  const ScanInfo(this.info);
}

/// "Info voucher" mode only: the scanned code is NOT a voucher (a billet or an
/// unknown code), so we refuse to query the external commercial service.
class ScanWrongType extends ScanState {
  final String code;
  const ScanWrongType(this.code);
}

/// Transport failure (NOT a verdict). The operator sees a retry screen.
class ScanError extends ScanState {
  final ApiException error;
  final String code;
  const ScanError(this.error, this.code);
}

class ScanController extends ChangeNotifier {
  final VerificationRepository _repo;
  ScanController(this._repo);

  ScanState _state = const ScanIdle();
  ScanState get state => _state;

  ScanIntent intent = ScanIntent.access;
  TicketKind ticketKind = TicketKind.auto;

  /// True while a request is in flight — used to gate the camera so we don't
  /// fire a second request for the same frame burst.
  bool _busy = false;
  bool get isBusy => _busy;

  void setIntent(ScanIntent value) {
    if (intent == value) return;
    intent = value;
    notifyListeners();
  }

  void setTicketKind(TicketKind value) {
    if (ticketKind == value) return;
    ticketKind = value;
    notifyListeners();
  }

  /// Called by the scanner when a barcode is decoded.
  Future<void> onCodeScanned(String code) async {
    if (_busy) return;
    final trimmed = code.trim();
    if (trimmed.isEmpty) return;

    _busy = true;
    _setState(ScanLoading(trimmed));

    try {
      if (intent == ScanIntent.access) {
        // Feature 2 — gate verdict (billet or voucher per the toggle).
        final result = await _repo.verifyAccess(trimmed, ticketKind);
        await FeedbackService.I.forVerdict(result.verdict);
        _setState(ScanResult(result));
      } else {
        // Feature 1 — "Info voucher". VOUCHERS ONLY: confirm the code is a
        // voucher in our DB first; a billet/unknown code is rejected with the
        // "voucher only" screen instead of a meaningless external lookup.
        final asVoucher = await _repo.verifyAccess(trimmed, TicketKind.voucher);
        if (asVoucher.verdict == Verdict.notFound) {
          await FeedbackService.I.forError();
          _setState(ScanWrongType(trimmed));
        } else {
          final info = await _repo.voucherInfo(trimmed);
          _setState(ScanInfo(info));
        }
      }
    } on ApiException catch (e) {
      await FeedbackService.I.forError();
      _setState(ScanError(e, trimmed));
    } catch (e) {
      await FeedbackService.I.forError();
      _setState(ScanError(const ApiException(ApiErrorKind.unknown, 'Unexpected'), trimmed));
    } finally {
      _busy = false;
    }
  }

  /// Retry the last failed code without rescanning.
  Future<void> retry(String code) => onCodeScanned(code);

  /// Back to a hot camera for the next scan.
  void reset() {
    _busy = false;
    _setState(const ScanIdle());
  }

  void _setState(ScanState s) {
    _state = s;
    notifyListeners();
  }
}
import '../domain/scan_mode.dart';
import '../domain/ticket_details.dart';
import '../domain/verdict.dart';
import '../domain/verification_result.dart';
import '../domain/voucher_info.dart';
import 'verification_remote_data_source.dart';

/// Orchestrates the verification use-cases over the data source.
class VerificationRepository {
  final VerificationRemoteDataSource _remote;
  VerificationRepository(this._remote);

  /// Feature 2 — gate verdict. Resolves billet vs voucher per [kind]; auto tries
  /// /billet then falls back to /voucher on NOT_FOUND.
  Future<VerificationResult> verifyAccess(String code, TicketKind kind) async {
    switch (kind) {
      case TicketKind.billet:
        return _remote.verifyBillet(code);
      case TicketKind.voucher:
        return _remote.verifyVoucher(code);
      case TicketKind.auto:
        final billet = await _remote.verifyBillet(code);
        if (billet.verdict != Verdict.notFound) return billet;
        return _remote.verifyVoucher(code);
    }
  }

  /// Feature 1 — commercial info via the external service (stub for now).
  Future<VoucherInfo> voucherInfo(String code) => _remote.voucherInfo(code);

  /// Lazy details (access log + extras), keyed by numéro de série. Loaded only
  /// when the ℹ screen opens. [type] is "BILLET" or "VOUCHER".
  Future<TicketDetails> ticketDetails(String type, String numeroserie) {
    return type == 'VOUCHER'
        ? _remote.voucherDetails(numeroserie)
        : _remote.billetDetails(numeroserie);
  }
}

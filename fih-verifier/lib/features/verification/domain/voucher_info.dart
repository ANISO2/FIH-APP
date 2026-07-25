/// Result of the external TICKET-VERIFY lookup shown on the "Info voucher"
/// screen. The backend calls festivaldehammamet.com/ticket/verify server-side
/// (the API key never reaches the device) and maps the response into this shape.
///
/// This is an INFO screen, not a verdict: the gate access decision lives in the
/// billet/voucher verification flow. Here we just surface who the ticket belongs
/// to and whether it has already been used.
enum VoucherInfoStatus {
  ok,
  notFound,
  unavailable,
  error;

  static VoucherInfoStatus fromApi(String? raw) {
    switch (raw) {
      case 'OK':
        return VoucherInfoStatus.ok;
      case 'NOT_FOUND':
        return VoucherInfoStatus.notFound;
      case 'ERROR':
        return VoucherInfoStatus.error;
      case 'UNAVAILABLE':
      case 'PENDING_INTEGRATION': // legacy: service not configured
      default:
        return VoucherInfoStatus.unavailable;
    }
  }
}

class VoucherInfo {
  final VoucherInfoStatus status;
  final String? code;
  final String? message;

  // External payload.
  final bool? used;
  final String? usedDate; // raw string from the external service
  final String? ticket;
  final String? ticketCin;
  final String? prenom;
  final String? nom;

  const VoucherInfo({
    required this.status,
    required this.code,
    required this.message,
    required this.used,
    required this.usedDate,
    required this.ticket,
    required this.ticketCin,
    required this.prenom,
    required this.nom,
  });

  bool get isOk => status == VoucherInfoStatus.ok;
  bool get isNotFound => status == VoucherInfoStatus.notFound;
  bool get isUnavailable => status == VoucherInfoStatus.unavailable;
  bool get isError => status == VoucherInfoStatus.error;

  /// Full holder name, when the parts are present.
  String? get holder {
    final parts = [prenom, nom].where((p) => p != null && p.trim().isNotEmpty).toList();
    return parts.isEmpty ? null : parts.join(' ');
  }

  factory VoucherInfo.fromJson(Map<String, dynamic> j) {
    return VoucherInfo(
      status: VoucherInfoStatus.fromApi(j['status'] as String?),
      code: j['code'] as String?,
      message: j['message'] as String?,
      used: j['used'] as bool?,
      usedDate: j['usedDate'] as String?,
      ticket: j['ticket'] as String?,
      ticketCin: j['ticketCin'] as String?,
      prenom: j['prenom'] as String?,
      nom: j['nom'] as String?,
    );
  }
}

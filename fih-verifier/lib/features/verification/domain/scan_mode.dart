/// What the operator is scanning FOR. Selected before scanning; the camera
/// surface is shared, only the result differs.
enum ScanIntent {
  /// Feature 2 — gate verdict against OUR database (billet or voucher).
  access,

  /// Feature 1 — commercial info about a paid voucher via the EXTERNAL service.
  info;

  String get frenchLabel => this == ScanIntent.access ? 'Contrôle d\'accès' : 'Info voucher';
}

/// For the access intent, which endpoint to hit. There is no reliable way to
/// tell a billet code from a voucher code from the code alone, so:
///   - [auto]   : try /billet, fall back to /voucher on NOT_FOUND (just scan).
///   - [billet] : force the billet endpoint (saves the fallback round trip).
///   - [voucher]: force the voucher endpoint.
/// Default is [auto] so the operator simply scans.
enum TicketKind {
  auto,
  billet,
  voucher;

  String get frenchLabel {
    switch (this) {
      case TicketKind.auto:
        return 'Auto';
      case TicketKind.billet:
        return 'Billet';
      case TicketKind.voucher:
        return 'Voucher';
    }
  }
}

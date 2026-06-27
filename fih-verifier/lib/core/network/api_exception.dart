/// A network/transport failure — distinct from a ticket VERDICT.
///
/// IMPORTANT distinction for the gate UX:
///   - A VERDICT (VALID / ALREADY_USED / NOT_ACTIVE / CANCELLED / NOT_FOUND) is
///     a successful answer from the server (always HTTP 200). It is NOT an error.
///   - An [ApiException] is the app failing to GET an answer at all: timeout,
///     no connectivity, a 5xx, a 401 (bad/missing device token), or unparseable
///     JSON. The scanner shows a distinct "réessayer" state for these, never a
///     red verdict — telling the operator a ticket is invalid when the network
///     merely hiccuped would be wrong and dangerous at a turnstile.
enum ApiErrorKind { timeout, offline, unauthorized, server, decode, unknown }

class ApiException implements Exception {
  final ApiErrorKind kind;
  final String message;
  final int? statusCode;

  const ApiException(this.kind, this.message, {this.statusCode});

  /// French label shown to the operator.
  String get frenchTitle {
    switch (kind) {
      case ApiErrorKind.timeout:
        return 'Délai dépassé';
      case ApiErrorKind.offline:
        return 'Hors ligne';
      case ApiErrorKind.unauthorized:
        return 'Accès refusé';
      case ApiErrorKind.server:
        return 'Erreur serveur';
      case ApiErrorKind.decode:
        return 'Réponse illisible';
      case ApiErrorKind.unknown:
        return 'Erreur';
    }
  }

  String get frenchHint {
    switch (kind) {
      case ApiErrorKind.timeout:
      case ApiErrorKind.offline:
        return 'Vérifiez le Wi-Fi puis réessayez.';
      case ApiErrorKind.unauthorized:
        return 'Jeton appareil invalide. Contactez l\'administrateur.';
      case ApiErrorKind.server:
        return 'Le serveur a renvoyé une erreur. Réessayez.';
      case ApiErrorKind.decode:
        return 'Format inattendu. Réessayez.';
      case ApiErrorKind.unknown:
        return 'Réessayez.';
    }
  }

  @override
  String toString() => 'ApiException($kind, $statusCode): $message';
}

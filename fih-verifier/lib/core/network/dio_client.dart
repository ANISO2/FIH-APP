import 'package:dio/dio.dart';

import '../config/app_config.dart';
import 'api_exception.dart';

/// One shared Dio instance for the whole app.
///
/// Beginner note: Dio is an HTTP client (like a nicer `http` package). We
/// configure it ONCE here so every request automatically:
///   1. carries the X-Device-Token header (our "no-login" auth), and
///   2. fails FAST — short timeouts so a scanner at a flaky gate gets a quick
///      error and retries, instead of hanging. These mirror the backend's
///      Hikari connection-timeout philosophy (3s, not 30s).
class DioClient {
  DioClient._() {
    _dio = Dio(
      BaseOptions(
        connectTimeout: const Duration(seconds: 3),
        sendTimeout: const Duration(seconds: 3),
        receiveTimeout: const Duration(seconds: 4),
        responseType: ResponseType.json,
        // The backend returns HTTP 200 for verdicts (incl. NOT_FOUND). We treat
        // any 2xx as success and let the repository read the body; 4xx/5xx flow
        // through onError below.
        validateStatus: (status) => status != null && status >= 200 && status < 300,
        headers: const {
          'Accept': 'application/json',
        },
      ),
    );

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          // The single shared device token — this is the whole "auth" for the app.
          options.headers['X-Device-Token'] = AppConfig.deviceToken;
          handler.next(options);
        },
      ),
    );
  }

  static final DioClient I = DioClient._();

  late final Dio _dio;
  Dio get dio => _dio;

  /// Maps a DioException to our typed [ApiException]. Repositories call this so
  /// the UI gets a clean, French-labelled failure (never a raw stack trace).
  static ApiException mapError(Object error) {
    if (error is ApiException) return error;
    if (error is DioException) {
      switch (error.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.sendTimeout:
        case DioExceptionType.receiveTimeout:
          return const ApiException(ApiErrorKind.timeout, 'Request timed out');
        case DioExceptionType.connectionError:
          return const ApiException(ApiErrorKind.offline, 'No connectivity');
        case DioExceptionType.badResponse:
          final code = error.response?.statusCode;
          if (code == 401 || code == 403) {
            return ApiException(ApiErrorKind.unauthorized, 'Unauthorized', statusCode: code);
          }
          return ApiException(ApiErrorKind.server, 'HTTP $code', statusCode: code);
        case DioExceptionType.cancel:
          return const ApiException(ApiErrorKind.unknown, 'Cancelled');
        case DioExceptionType.badCertificate:
        case DioExceptionType.unknown:
          return const ApiException(ApiErrorKind.unknown, 'Unknown error');
      }
    }
    return ApiException(ApiErrorKind.unknown, error.toString());
  }
}

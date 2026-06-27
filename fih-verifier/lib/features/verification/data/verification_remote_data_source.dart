import 'package:dio/dio.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../domain/ticket_details.dart';
import '../domain/verification_result.dart';
import '../domain/voucher_info.dart';

/// Talks to fih-companion-api. One method per endpoint. Maps transport failures
/// to [ApiException]; a NOT_FOUND verdict is a normal 200 body, NOT an error.
///
/// The scanned code is sent via `queryParameters` (?code=...), so Dio encodes it
/// safely and any QR/barcode content (slashes, newlines, URLs) is handled
/// without tripping the server. We also strip control characters a scanner may
/// append, without altering the meaningful characters of the code.
class VerificationRemoteDataSource {
  final Dio _dio;
  VerificationRemoteDataSource(this._dio);

  Future<VerificationResult> verifyBillet(String code) =>
      _getJson(ApiEndpoints.verifyBillet(), VerificationResult.fromJson, code: code);

  Future<VerificationResult> verifyVoucher(String code) =>
      _getJson(ApiEndpoints.verifyVoucher(), VerificationResult.fromJson, code: code);

  Future<VoucherInfo> voucherInfo(String code) =>
      _getJson(ApiEndpoints.voucherInfo(), VoucherInfo.fromJson, code: code);

  Future<TicketDetails> billetDetails(String numeroserie) =>
      _getJson(ApiEndpoints.billetDetails(numeroserie), TicketDetails.fromJson);

  Future<TicketDetails> voucherDetails(String numeroserie) =>
      _getJson(ApiEndpoints.voucherDetails(numeroserie), TicketDetails.fromJson);

  Future<T> _getJson<T>(
    String url,
    T Function(Map<String, dynamic>) parse, {
    String? code,
  }) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>(
        url,
        queryParameters: code == null ? null : {'code': _clean(code)},
      );
      final data = res.data;
      if (data == null) {
        throw const ApiException(ApiErrorKind.decode, 'Empty body');
      }
      return parse(data);
    } catch (e) {
      throw DioClient.mapError(e);
    }
  }

  /// Remove whitespace and invisible control characters a scanner can append
  /// (trailing newline/tab, NUL, zero-width space, BOM). The meaningful
  /// characters of the code are untouched.
  static String _clean(String code) =>
      code.replaceAll(RegExp(r'[\u0000-\u001F\u007F\u200B\uFEFF]'), '').trim();
}
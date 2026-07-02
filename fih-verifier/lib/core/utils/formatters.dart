import 'package:intl/intl.dart';

/// Locale-aware formatting helpers. French (fr_FR), money in TND.
class Formatters {
  Formatters._();

  static final NumberFormat _money = NumberFormat.currency(
    locale: 'fr_FR',
    symbol: 'TND',
    decimalDigits: 3, // TND has 3 decimals (millimes)
  );

  static final DateFormat _longDate = DateFormat('EEEE d MMMM yyyy', 'fr_FR');
  static final DateFormat _shortDate = DateFormat('d MMM yyyy', 'fr_FR');
  static final DateFormat _time = DateFormat('HH:mm:ss', 'fr_FR');
  static final DateFormat _dayMonth = DateFormat('dd/MM/yyyy', 'fr_FR');

  static String money(num? value) => value == null ? '—' : _money.format(value);
  static String longDate(DateTime? d) => d == null ? '—' : _longDate.format(d);
  static String shortDate(DateTime? d) => d == null ? '—' : _shortDate.format(d);
  static String dayMonth(DateTime? d) => d == null ? '—' : _dayMonth.format(d);
  static String time(DateTime? d) => d == null ? '—' : _time.format(d);

  /// Parse a backend ISO date/datetime string. Spring serializes
  /// LocalDate/LocalDateTime as ISO-8601 strings, so DateTime.tryParse works.
  ///
  /// A missing/zero legacy value surfaces as the Unix epoch (1970-01-01) once
  /// parsed, which would otherwise render as "01/01/1970". We treat any
  /// epoch/zero date as "no date" (null) so every screen shows "—" instead.
  /// The festival has no real pre-1971 dates, so `year <= 1970` is a safe
  /// sentinel. This is the single chokepoint: every date in the app is parsed
  /// here, so nulling it once hides 1970 everywhere.
  static DateTime? parseDate(Object? raw) {
    if (raw == null) return null;
    if (raw is String && raw.isNotEmpty) {
      final parsed = DateTime.tryParse(raw);
      if (parsed == null || parsed.year <= 1970) return null;
      return parsed;
    }
    return null;
  }
}

import 'package:flutter/material.dart';

/// Design tokens for FIH Verifier.
///
/// Brand: primary #1F5F8B, accent #0F9D9D (aligned with the backoffice).
/// Semantic verdict colors are deliberately chosen (not seed-derived) so the
/// "stop" red is unmistakably alarming and VALID green reads from arm's length
/// in bright sunlight.
class AppColors {
  AppColors._();

  static const Color primary = Color(0xFF1F5F8B);
  static const Color accent = Color(0xFF0F9D9D);
  static const Color surface = Color(0xFFF4F7FA);

  // Full-bleed verdict backgrounds.
  static const Color verdictValid = Color(0xFF1B873F); // VALID -> go
  static const Color verdictWarn = Color(0xFFC77700);  // ALREADY_USED / NOT_ACTIVE
  static const Color verdictStop = Color(0xFFC62828);  // CANCELLED / NOT_FOUND

  static const Color onVerdict = Colors.white;
}

/// Spacing on an 8-pt grid.
class Gap {
  Gap._();
  static const double xs = 4;
  static const double sm = 8;
  static const double md = 16;
  static const double lg = 24;
  static const double xl = 32;
}

ThemeData buildAppTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: AppColors.primary,
    primary: AppColors.primary,
    secondary: AppColors.accent,
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.surface,
    appBarTheme: const AppBarTheme(centerTitle: true),
    cardTheme: CardThemeData(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(56),
        textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: Colors.white,
      indicatorColor: AppColors.accent.withValues(alpha: 0.18),
      labelTextStyle: WidgetStateProperty.all(
        const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
      ),
    ),
  );
}

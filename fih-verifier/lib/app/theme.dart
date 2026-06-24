import 'package:flutter/material.dart';

/// Base theme, aligned with the backoffice brand (primary #1F5F8B, accent #0F9D9D).
/// Kept intentionally minimal; refine during implementation.
ThemeData buildAppTheme() {
  const primary = Color(0xFF1F5F8B);
  return ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(seedColor: primary),
    scaffoldBackgroundColor: const Color(0xFFF4F7FA),
    appBarTheme: const AppBarTheme(centerTitle: true),
  );
}

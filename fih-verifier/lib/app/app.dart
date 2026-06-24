import 'package:flutter/material.dart';

import '../core/config/app_constants.dart';
import '../features/verification/presentation/pages/home_page.dart';
import 'theme.dart';

/// Root of the FIH Verifier app. Base scaffold only — the real navigation and
/// verification screens are added in the implementation phase.
class FihVerifierApp extends StatelessWidget {
  const FihVerifierApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: AppConstants.appName,
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: const HomePage(),
    );
  }
}

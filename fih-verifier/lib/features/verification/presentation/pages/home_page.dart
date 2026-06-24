import 'package:flutter/material.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/config/app_constants.dart';

/// Placeholder landing screen. Confirms the base app builds and runs on a
/// device. The scanner + verdict UI replaces this in the implementation phase.
class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text(AppConstants.appName)),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.qr_code_scanner, size: 72, color: theme.colorScheme.primary),
            const SizedBox(height: 16),
            Text('Base prête', style: theme.textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('API : ${AppConfig.apiBaseUrl}', style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}

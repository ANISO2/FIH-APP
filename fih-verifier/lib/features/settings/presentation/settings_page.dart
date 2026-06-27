import 'package:flutter/material.dart';

import '../../../app/theme.dart';
import '../../../core/config/app_config.dart';
import '../../../core/feedback/feedback_service.dart';

/// Minimal settings: shows the active API base URL (set via --dart-define),
/// confirms the no-login device-token model, and lets the operator mute
/// sound/haptic. Kept simple for the gate.
class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  @override
  Widget build(BuildContext context) {
    final fb = FeedbackService.I;
    final maskedToken = AppConfig.deviceToken.isEmpty
        ? '—'
        : '${AppConfig.deviceToken.substring(0, AppConfig.deviceToken.length.clamp(0, 3))}•••';

    return Scaffold(
      appBar: AppBar(title: const Text('Réglages')),
      body: ListView(
        padding: const EdgeInsets.all(Gap.md),
        children: [
          _section('Connexion'),
          _infoTile(Icons.link_rounded, 'API', AppConfig.apiBaseUrl),
          _infoTile(Icons.vpn_key_rounded, 'Jeton appareil', maskedToken,
              hint: 'Authentification sans login (en-tête X-Device-Token).'),
          const SizedBox(height: Gap.md),
          _section('Retour scan'),
          SwitchListTile(
            value: fb.soundEnabled,
            onChanged: (v) => setState(() => fb.soundEnabled = v),
            title: const Text('Son'),
            secondary: const Icon(Icons.volume_up_rounded),
          ),
          SwitchListTile(
            value: fb.hapticEnabled,
            onChanged: (v) => setState(() => fb.hapticEnabled = v),
            title: const Text('Vibration'),
            secondary: const Icon(Icons.vibration_rounded),
          ),
        ],
      ),
    );
  }

  Widget _section(String title) => Padding(
        padding: const EdgeInsets.only(bottom: Gap.sm, left: Gap.xs),
        child: Text(title.toUpperCase(),
            style: const TextStyle(
                color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: 13, letterSpacing: 0.5)),
      );

  Widget _infoTile(IconData icon, String label, String value, {String? hint}) => Card(
        child: ListTile(
          leading: Icon(icon, color: AppColors.primary),
          title: Text(label),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
              if (hint != null)
                Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Text(hint, style: const TextStyle(fontSize: 12)),
                ),
            ],
          ),
        ),
      );
}

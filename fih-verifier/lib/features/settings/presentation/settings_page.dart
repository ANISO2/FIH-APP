import 'package:flutter/material.dart';

import '../../../app/theme.dart';
import '../../../core/config/app_config.dart';
import '../../../core/feedback/feedback_service.dart';

/// Réglages: lets the operator point the APK at a different backend at runtime
/// (persisted on the device, applied immediately), shows the no-login
/// device-token model, and toggles sound/haptic. Kept simple for the gate.
class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  late final TextEditingController _apiCtrl;
  bool _dirty = false;

  @override
  void initState() {
    super.initState();
    _apiCtrl = TextEditingController(text: AppConfig.apiBaseUrl)
      ..addListener(() {
        final d = _apiCtrl.text.trim() != AppConfig.apiBaseUrl;
        if (d != _dirty) setState(() => _dirty = d);
      });
  }

  @override
  void dispose() {
    _apiCtrl.dispose();
    super.dispose();
  }

  Future<void> _saveApi() async {
    final value = _apiCtrl.text.trim();
    if (value.isEmpty) return;
    await AppConfig.setApiBaseUrl(value);
    // Reflect the normalized (trailing-slash-trimmed) value back into the field.
    _apiCtrl.text = AppConfig.apiBaseUrl;
    if (!mounted) return;
    setState(() => _dirty = false);
    FocusScope.of(context).unfocus();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('API enregistrée : ${AppConfig.apiBaseUrl}')),
    );
  }

  void _resetApi() {
    _apiCtrl.text = AppConfig.defaultApiBaseUrl;
    setState(() => _dirty = _apiCtrl.text.trim() != AppConfig.apiBaseUrl);
  }

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
          _apiCard(),
          const SizedBox(height: Gap.sm),
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

  // ----------------------------------------------------------- API base URL
  Widget _apiCard() => Card(
        child: Padding(
          padding: const EdgeInsets.all(Gap.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: const [
                  Icon(Icons.link_rounded, color: AppColors.primary),
                  SizedBox(width: Gap.sm),
                  Text('API', style: TextStyle(fontWeight: FontWeight.w700)),
                ],
              ),
              const SizedBox(height: Gap.sm),
              TextField(
                controller: _apiCtrl,
                keyboardType: TextInputType.url,
                autocorrect: false,
                enableSuggestions: false,
                style: const TextStyle(fontWeight: FontWeight.w600),
                decoration: const InputDecoration(
                  isDense: true,
                  border: OutlineInputBorder(),
                  hintText: 'http://192.168.1.10:8080',
                  labelText: 'Adresse du serveur (base URL)',
                ),
                onSubmitted: (_) => _saveApi(),
              ),
              const SizedBox(height: 6),
              const Text(
                "Pointez l'APK vers un autre backend sans recompiler. Appliqué "
                "immédiatement aux prochaines requêtes.",
                style: TextStyle(fontSize: 12),
              ),
              const SizedBox(height: Gap.sm),
              Row(
                children: [
                  TextButton.icon(
                    onPressed: _resetApi,
                    icon: const Icon(Icons.restore_rounded, size: 18),
                    label: const Text('Défaut'),
                  ),
                  const Spacer(),
                  FilledButton.icon(
                    onPressed: _dirty ? _saveApi : null,
                    icon: const Icon(Icons.save_rounded, size: 18),
                    label: const Text('Enregistrer'),
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      minimumSize: const Size(0, 44),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      );

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

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'core/config/app_config.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Load the persisted backend API base URL (set in Réglages) before anything
  // builds a request, so the very first call already uses the chosen backend.
  await AppConfig.load();
  // Load French date symbols for intl (DateFormat 'fr_FR').
  await initializeDateFormatting('fr_FR', null);
  // Gate app is used one-handed, upright.
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(const FihVerifierApp());
}

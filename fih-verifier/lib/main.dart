import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Load French date symbols for intl (DateFormat 'fr_FR').
  await initializeDateFormatting('fr_FR', null);
  // Gate app is used one-handed, upright.
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(const FihVerifierApp());
}

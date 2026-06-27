import 'package:flutter/material.dart';

import '../../../settings/presentation/settings_page.dart';
import '../../../stats/presentation/pages/stats_dashboard_page.dart';
import 'scanner_page.dart';

/// Root shell with bottom navigation. (Was the "Base prête" placeholder; now the
/// real shell. Kept the class name `HomePage` so app.dart's `home:` is unchanged.)
///
/// Tabs: Scanner (Feature 2/1), Statistiques (Feature 3), Réglages.
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _index = 0;

  // IndexedStack keeps each tab alive — the camera isn't torn down when you peek
  // at stats and come back.
  static const _pages = [
    ScannerPage(),
    StatsDashboardPage(),
    SettingsPage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(
              icon: Icon(Icons.qr_code_scanner_outlined),
              selectedIcon: Icon(Icons.qr_code_scanner_rounded),
              label: 'Scanner'),
          NavigationDestination(
              icon: Icon(Icons.insights_outlined),
              selectedIcon: Icon(Icons.insights_rounded),
              label: 'Stats'),
          NavigationDestination(
              icon: Icon(Icons.settings_outlined),
              selectedIcon: Icon(Icons.settings_rounded),
              label: 'Réglages'),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../../../settings/presentation/settings_page.dart';
import '../../../stats/presentation/pages/stats_dashboard_page.dart';
import 'scanner_page.dart';

/// Root shell with bottom navigation. (Was the "Base prête" placeholder; now the
/// real shell. Kept the class name `HomePage` so app.dart's `home:` is unchanged.)
///
/// Tabs: Scanner (Feature 2/1), Statistiques (Feature 3), Réglages.
///
/// STARTUP JANK — "Skipped 283 frames!"
/// ------------------------------------
/// `IndexedStack` builds EVERY child on the first frame, so all three tabs used
/// to initialise at launch, at once, before anything was on screen:
///   * ScannerPage.initState  -> MobileScannerController -> CameraX + ML Kit
///                               (barhopper .so load, TFLite/XNNPACK init) ;
///   * StatsDashboardPage.initState -> StatsController.init() -> TWO network
///                               round-trips + a 15s Timer.periodic, for a tab
///                               the operator isn't even looking at ;
///   * SettingsPage.initState -> a TextEditingController whose field could grab
///                               focus and raise the IME at launch.
/// On top of main()'s three pre-runApp awaits, that produced ~3.1s of latency
/// and the 283 dropped frames.
///
/// Tabs are therefore built LAZILY and then KEPT ALIVE. The Scanner (index 0) is
/// still built immediately — it's the point of the app — while Statistiques and
/// Réglages are created the first time they're selected. Once a tab has been
/// visited it stays in the IndexedStack, so the original intent holds: the camera
/// is NOT torn down when you peek at the stats and come back.
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const int _tabCount = 3;

  int _index = 0;

  /// Indices already visited. The Scanner starts built; the others join on first
  /// selection and never leave — that's what preserves their State (and the live
  /// camera) across tab switches.
  final Set<int> _built = <int>{0};

  /// Const constructors: rebuilding the same tab yields an identical widget of
  /// the same runtimeType, so Flutter reuses the Element and the State survives.
  Widget _pageAt(int i) {
    switch (i) {
      case 0:
        return const ScannerPage();
      case 1:
        return const StatsDashboardPage();
      default:
        return const SettingsPage();
    }
  }

  void _select(int i) {
    if (i == _index) return;
    setState(() {
      _built.add(i);
      _index = i;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _index,
        children: List<Widget>.generate(
          _tabCount,
          // A not-yet-visited tab is an empty placeholder: zero cost, and it
          // keeps the children list length/order aligned with `index`.
          (i) => _built.contains(i) ? _pageAt(i) : const SizedBox.shrink(),
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _select,
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

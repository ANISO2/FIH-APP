import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../../../app/theme.dart';
import '../../../../core/network/dio_client.dart';
import '../../data/verification_remote_data_source.dart';
import '../../data/verification_repository.dart';
import '../../domain/scan_mode.dart';
import '../controllers/scan_controller.dart';
import '../widgets/result_sheet.dart';
import '../widgets/scanner_overlay.dart';
import '../widgets/wrong_type_view.dart';
import 'ticket_details_page.dart';
import 'voucher_info_page.dart';

/// The gate screen. One shared camera; two intents (access / info).
///
/// FREEZE / SPEED
/// --------------
///   1. Detect EVERY barcode/QR format (no `formats` restriction).
///   2. STOP the camera the instant a code is read, and restart only on
///      "Scanner suivant". This clears the analyzer between scans (no backlog →
///      no freeze after a few tickets) and frees the CPU (snappier UI).
///   3. Lower analysis resolution (720p) → ML detection runs faster on phones.
///   4. An instant haptic the moment a code is captured → feels immediate even
///      while the network call is still in flight.
///   5. Pause the camera when the app is backgrounded; resume when it returns.
///
/// All mobile_scanner API usage stays isolated to THIS file.
class ScannerPage extends StatefulWidget {
  const ScannerPage({super.key});

  @override
  State<ScannerPage> createState() => _ScannerPageState();
}

class _ScannerPageState extends State<ScannerPage> with WidgetsBindingObserver {
  late final ScanController _scan;
  late final MobileScannerController _camera;

  // Serialises start/stop so a rapid stop→start can't race the platform side.
  bool _switching = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    final repo = VerificationRepository(VerificationRemoteDataSource(DioClient.I.dio));
    _scan = ScanController(repo);
    _camera = MobileScannerController(
      // No `formats:` list → detect ALL supported symbologies (QR, Code128,
      // EAN-8/13, UPC, Code39/93, ITF, Codabar, Data Matrix, PDF417, Aztec…).
      detectionSpeed: DetectionSpeed.noDuplicates,
      detectionTimeoutMs: 250,
      // 720p analysis: plenty for close-range gate scanning, much lighter on the
      // CPU than full sensor resolution → faster detection, snappier UI.
      cameraResolution: const Size(1280, 720),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _camera.dispose();
    _scan.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!mounted) return;
    switch (state) {
      case AppLifecycleState.resumed:
        if (_scan.state is ScanIdle) _safeStart();
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.hidden:
      case AppLifecycleState.detached:
        _safeStop();
        break;
    }
  }

  Future<void> _safeStart() async {
    if (_switching) return;
    _switching = true;
    try {
      await _camera.start();
    } catch (_) {
      // Already starting/started — ignore.
    } finally {
      _switching = false;
    }
  }

  Future<void> _safeStop() async {
    if (_switching) return;
    _switching = true;
    try {
      await _camera.stop();
    } catch (_) {
      // Already stopped — ignore.
    } finally {
      _switching = false;
    }
  }

  void _onDetect(BarcodeCapture capture) {
    if (_scan.state is! ScanIdle || _scan.isBusy) return;
    if (capture.barcodes.isEmpty) return;
    final code = capture.barcodes.first.rawValue;
    if (code == null || code.trim().isEmpty) return;

    HapticFeedback.selectionClick(); // instant "got it" feedback
    _safeStop(); // stop analysing immediately so the pipeline can't back up
    _scan.onCodeScanned(code);
  }

  /// Back to a hot camera for the next ticket.
  void _resume() {
    _scan.reset();
    _safeStart();
  }

  void _openDetails(ScanResult state) {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => TicketDetailsPage(result: state.result)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: ListenableBuilder(
        listenable: _scan,
        builder: (context, _) {
          final state = _scan.state;
          return Stack(
            fit: StackFit.expand,
            children: [
              MobileScanner(controller: _camera, onDetect: _onDetect, fit: BoxFit.cover),
              _topGradient(),
              ScannerOverlay(active: state is ScanIdle),
              _topBar(),
              if (state is ScanIdle) _controls(),
              if (state is ScanLoading) _loading(),
              if (state is ScanResult)
                VerdictResultView(
                  result: state.result,
                  onNext: _resume,
                  onDetails: () => _openDetails(state),
                ),
              if (state is ScanInfo)
                VoucherInfoView(info: state.info, onNext: _resume),
              if (state is ScanWrongType) WrongTypeView(onNext: _resume),
              if (state is ScanError)
                ScanErrorView(
                  error: state.error,
                  onRetry: () => _scan.retry(state.code), // camera stays stopped during retry
                  onCancel: _resume,
                ),
            ],
          );
        },
      ),
    );
  }

  Widget _topGradient() => Positioned(
        top: 0,
        left: 0,
        right: 0,
        height: 140,
        child: IgnorePointer(
          child: DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [Colors.black.withValues(alpha: 0.55), Colors.transparent],
              ),
            ),
          ),
        ),
      );

  Widget _topBar() => SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Gap.md, vertical: Gap.sm),
          child: Row(
            children: [
              const Text('Contrôle',
                  style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w700)),
              const Spacer(),
              ValueListenableBuilder<MobileScannerState>(
                valueListenable: _camera,
                builder: (context, mState, _) {
                  final on = mState.torchState == TorchState.on;
                  return IconButton.filledTonal(
                    onPressed: () => _camera.toggleTorch(),
                    icon: Icon(on ? Icons.flash_on_rounded : Icons.flash_off_rounded),
                    tooltip: 'Lampe',
                  );
                },
              ),
            ],
          ),
        ),
      );

  Widget _loading() => Container(
        color: Colors.black.withValues(alpha: 0.45),
        child: const Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 56,
                height: 56,
                child: CircularProgressIndicator(color: Colors.white, strokeWidth: 5),
              ),
              SizedBox(height: Gap.md),
              Text('Vérification…',
                  style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600)),
            ],
          ),
        ),
      );

  Widget _controls() => Align(
        alignment: Alignment.bottomCenter,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(Gap.md),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                SegmentedButton<ScanIntent>(
                  segments: const [
                    ButtonSegment(value: ScanIntent.access, label: Text('Contrôle d\'accès')),
                    ButtonSegment(value: ScanIntent.info, label: Text('Info voucher')),
                  ],
                  selected: {_scan.intent},
                  onSelectionChanged: (s) => _scan.setIntent(s.first),
                  style: ButtonStyle(
                    backgroundColor: WidgetStateProperty.resolveWith(
                      (states) => states.contains(WidgetState.selected)
                          ? AppColors.primary
                          : Colors.white.withValues(alpha: 0.92),
                    ),
                    foregroundColor: WidgetStateProperty.resolveWith(
                      (states) => states.contains(WidgetState.selected)
                          ? Colors.white
                          : AppColors.primary,
                    ),
                  ),
                ),
                if (_scan.intent == ScanIntent.access) ...[
                  const SizedBox(height: Gap.sm),
                  SegmentedButton<TicketKind>(
                    segments: const [
                      ButtonSegment(value: TicketKind.auto, label: Text('Auto')),
                      ButtonSegment(value: TicketKind.billet, label: Text('Billet')),
                      ButtonSegment(value: TicketKind.voucher, label: Text('Voucher')),
                    ],
                    selected: {_scan.ticketKind},
                    onSelectionChanged: (s) => _scan.setTicketKind(s.first),
                    style: ButtonStyle(
                      backgroundColor:
                          WidgetStateProperty.all(Colors.white.withValues(alpha: 0.92)),
                      visualDensity: VisualDensity.compact,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      );
}
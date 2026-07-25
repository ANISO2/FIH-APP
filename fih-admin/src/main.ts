import { bootstrapApplication } from '@angular/platform-browser';
import * as echarts from 'echarts';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { registerFihTheme } from './app/shared/echarts-theme';

// ---------------------------------------------------------------------------
// Drop an EXPIRED JWT before Angular boots.
// A stale token left in localStorage from a previous session was being treated
// as "logged in": the dashboard loaded, fired its /api/stats calls with the
// dead token, the first 401 logged out + redirected to /login, and the rest
// aborted — the "freeze -> back to login" loop. Clearing it here (isolated from
// AuthService, so nothing in the guard chain changes) makes the app land
// cleanly on /login; a fresh login then works normally.
// ---------------------------------------------------------------------------
function purgeExpiredToken(): void {
  try {
    const token = localStorage.getItem('fih_token');
    if (!token) return;
    const parts = token.split('.');
    if (parts.length !== 3) return; // not a JWT we can read — leave it for the server
    let seg = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = seg.length % 4;
    if (pad) seg += '='.repeat(4 - pad);
    const payload = JSON.parse(atob(seg)) as { exp?: number };
    if (typeof payload.exp === 'number' && Date.now() >= payload.exp * 1000) {
      localStorage.removeItem('fih_token');
      localStorage.removeItem('fih_name');
      localStorage.removeItem('fih_role');
    }
  } catch {
    // Unreadable token — leave it; the server will reject it if invalid.
  }
}

purgeExpiredToken();

// Register the centralized ECharts brand theme once, before bootstrap.
registerFihTheme(echarts);

bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));

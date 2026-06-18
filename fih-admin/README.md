# fih-admin — FIH 2025 Admin (Angular)

Phase 5 · Part B. A read-only analytics dashboard for the FIH 2025 companion
backend. Angular 19 (standalone), Angular Material, Tailwind CSS, and
ngx-echarts. It only ever GETs stats — no write calls anywhere.

## Prerequisites
- Node.js 20+ and npm.
- The Spring Boot backend running on http://localhost:8080 with Part A
  (the /api/stats/** endpoints) in place.

## First-time setup
From inside fih-companion/fih-admin/:

    npm install

Downloads Angular, Material, Tailwind, ECharts (needs internet the first time).

## Run it
    npm start

Open http://localhost:4200 and log in with a real admin from the utilisateur
table (e.g. admin).

### Why no CORS setup is needed
npm start runs Angular with proxy.conf.json, which forwards every /api request
from :4200 to the backend at :8080. Same-origin to the browser, so no CORS issue
in development. For production, serve the built app behind the same origin or add
CORS rules.

## Organization
src/app/
  app.config.ts   providers: router, http + auth interceptor, echarts, animations
  app.routes.ts   routes; everything except /login is behind the auth guard
  core/           models, auth.service, auth.interceptor, auth.guard, stats.service
  shared/         echarts-theme, format pipes, kpi-card, chart-card, skeleton, empty-state
  layout/         shell.component (dark sidebar + top bar, collapses on mobile)
  pages/          login, overview, events, event-detail, gates

## New-concept notes
- Interceptor: a function every HTTP request passes through. Ours adds the JWT
  header and, on a 401, logs out and redirects to login.
- Route guard: runs before a page shows; authGuard returns the login URL if there
  is no token, else true.
- Standalone components: no NgModules; each lists its own imports. Pages are
  lazy-loaded with loadComponent in the routes.
- ECharts options: each chart is an options object; theme="fih" applies the shared
  brand styling so all charts match (colors in shared/echarts-theme.ts).
- Signals: signal(...) holds reactive state; templates re-render on change.

## Three states everywhere
Every data view shows a shimmer skeleton while loading, a friendly empty state
when there's nothing, and an error card if the backend is unreachable — never a
blank screen. Test the error path by stopping the backend and reloading.

## Acceptance checklist
- [ ] npm install then npm start; login works; bad credentials show a 401 message.
- [ ] Overview shows real KPIs and all four charts in the brand palette.
- [ ] Events table sorts and filters; a row opens event detail with its hourly curve.
- [ ] Gates separates Public/Vip and highlights rejections.
- [ ] Skeletons, empty states, and the error card are visible (stop the backend).
- [ ] Responsive: sidebar collapses to a hamburger; KPI cards reflow 4 -> 2 -> 1.

import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell.component';
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./pages/overview.component').then(m => m.OverviewComponent) },
      { path: 'events', loadComponent: () => import('./pages/events.component').then(m => m.EventsComponent) },
      { path: 'events/:id', loadComponent: () => import('./pages/event-detail.component').then(m => m.EventDetailComponent) },
      { path: 'recette', loadComponent: () => import('./pages/recette.component').then(m => m.RecetteComponent) },
      { path: 'gates', loadComponent: () => import('./pages/gates.component').then(m => m.GatesComponent) },
      { path: 'badges', loadComponent: () => import('./pages/badges.component').then(m => m.BadgesComponent) },
      { path: 'badges/:eventId/:modelId', loadComponent: () => import('./pages/badge-detail.component').then(m => m.BadgeDetailComponent) }
    ]
  },
  { path: '**', redirectTo: '' }
];

// TypeScript shapes mirroring the backend stats DTOs.

export interface Overview {
  totalEvents: number; totalBillets: number; totalVouchers: number;
  totalScans: number; acceptedScans: number; rejectedScans: number; acceptanceRate: number;
  publicScans: number; vipScans: number;
  busiestEventTitle: string | null; busiestEventDate: string | null; busiestEventScans: number;
}
export interface EntryByDay { date: string; scans: number; accepted: number; rejected: number; }
export interface GateBucket { scans: number; accepted: number; rejected: number; }
export interface Gate { public: GateBucket; vip: GateBucket; }
export interface TicketBucket { issued: number; scanned: number; }
export interface TicketTypes { billet: TicketBucket; voucher: TicketBucket; }
export interface EventRollup {
  eventId: number; title: string; date: string;
  scans: number; accepted: number; rejected: number; acceptanceRate: number;
  publicScans: number; vipScans: number;
}
export interface HourEntry { hour: number; scans: number; }
export interface EventDetail {
  eventId: number; title: string; date: string;
  scans: number; accepted: number; rejected: number; acceptanceRate: number;
  gate: Gate; entriesByHour: HourEntry[];
}
export interface LoginResponse { token: string; role: string; displayName: string; }

// ---- Phase 6: badges ----
export interface Availability {
  eventId: number; eventTitle: string; eventDate: string;
  modelId: number; modelName: string; accessZones: string[];
  injectedCount: number; billetCount: number; voucherCount: number;
  withPhotoCount: number | null; missingPhotoCount: number | null;
}
export interface BadgeItem {
  type: string; numeroserie: string; codebarre: string;
  holderName: string | null; affecteeA: string | null; hasPhoto: boolean;
}
/** Réponse de PUT /api/invitations/{numeroserie}/affectee (nom « Affectée à »). */
export interface Affectee {
  numeroserie: string; affecteeA: string; updatedAt: string; updatedBy: string | null;
}
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface PhotoCheck { total: number; withPhoto: number; missing: number; missingCodes: string[]; }

// ---- Recette (revenue) statistics ----
/** Recette résumé: revenue (TND) per event, split by category. */
export interface RecetteSummary {
  eventId: number; eventTitle: string; eventDate: string;
  billet: number; voucher: number; kit: number; total: number;
}
/** Recette détaillée: one row per (event x model). */
export interface RecetteDetail {
  eventId: number; eventTitle: string; eventDate: string;
  modelId: number; modelName: string; montant: number;
  voucherGeneration: number; voucherVente: number; voucherReste: number;
  billetGeneration: number; billetVente: number; billetReste: number;
  kitGeneration: number; kitVente: number; kitReste: number;
  total: number; recetteTnd: number;
}

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
  eventHasPoster: boolean;            // Change A: event has a poster file?
}
export interface BadgeItem {
  type: string; numeroserie: string; codebarre: string;
  holderName: string | null; affecteeA: string | null;
  printedAt: string | null;          // §6: when last printed (null = jamais)
}
/** Réponse de PUT /api/invitations/{numeroserie}/affectee (nom « Affectée à »). */
export interface Affectee {
  numeroserie: string; affecteeA: string; updatedAt: string; updatedBy: string | null;
  printedAt: string | null;
}
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }

// ---- Change A / §6: posters ----
export interface MissingPoster { eventId: number; eventTitle: string; eventDate: string; invitationCount: number; }

// ---- Change C: lot (affectation par plage de séries) ----
export interface LotRequest { startSerie: string; endSerie: string; baseName: string; }
export interface LotItem {
  numeroserie: string; codebarre: string;
  eventId: number; eventTitle: string; modelId: number; modelName: string;
  proposedName: string; alreadyAssigned: boolean; existingName: string | null;
}
export interface LotPreview {
  eligibleCount: number; alreadyAssignedCount: number; nonInvitationCount: number;
  baseNameAlreadyUsed: boolean; canAssign: boolean;
  items: LotItem[]; alreadyAssignedSerials: string[];
}
export interface LotResult { assignedCount: number; assigned: Affectee[]; }


// ---- Recette (revenue) statistics ----
/** Recette résumé: revenue (TND) per event, split Billet / Voucher (Kit removed). */
export interface RecetteSummary {
  eventId: number; eventTitle: string; eventDate: string;
  billet: number; voucher: number; total: number;
}
/** Recette détaillée — one collapsible panel header (per-event totals). */
export interface RecetteEventHeader {
  eventId: number; eventTitle: string; eventDate: string;
  totalGenere: number; totalVendu: number; totalReste: number;
  recetteTotale: number; tauxVente: number;
}
/** Recette détaillée — one per-model row, loaded on expand (Kit removed). */
export interface RecetteModelRow {
  modelId: number; modelName: string; montant: number;
  billetGeneration: number; billetVente: number; billetReste: number;
  voucherGeneration: number; voucherVente: number; voucherReste: number;
  totalVendu: number; recetteTnd: number; tauxVente: number;
}

// ---- Recette par guichet (§5.2) ----
/** Recette par guichet — résumé: guichet revenue (TND) per event. */
export interface RecetteGuichetSummary {
  eventId: number; eventTitle: string; eventDate: string;
  billet: number; kit: number; total: number;
}
/** Recette par guichet — détail: per (event x model) delivery + sales. */
export interface RecetteGuichetDetail {
  eventId: number; eventTitle: string; eventDate: string;
  modelId: number; modelName: string;
  billetLivraison: number; billetVente: number; billetPrixUnitaire: number;
  billetRecette: number; billetReste: number; kit: number;
}

// ---- Statistique des tourniquets (§5.3) ----
export interface TourniquetRow {
  modelId: number; modelName: string;
  billetCodes: number; voucherCodes: number; audience: number;
  billetTransactions: number; voucherTransactions: number;
}
export interface TourniquetEvent {
  eventId: number; eventTitle: string; eventDate: string;
  audience: number; transactionsBillets: number; transactionsVouchers: number;
  tourniquets: number; rows: TourniquetRow[];
}

// ---- Analyse des rejets (Part C) ----
export interface RejetGroupe { label: string; valeur: number; }
export interface RejetEvenement { eventId: number; eventTitle: string; eventDate: string; rejets: number; }
export interface RejetModele { modelId: number; modelName: string; rejets: number; }
export interface RejetJour { jour: string; rejets: number; }
export interface RejetScan { codebarre: string; eventTitle: string; porte: string; dateTime: string; description: string; }
export interface RejetsData {
  totalRejets: number; totalAcceptes: number; totalScans: number; tauxRejet: number;
  parCategorie: RejetGroupe[]; parEvenement: RejetEvenement[]; parPorte: RejetGroupe[];
  parModele: RejetModele[]; parJour: RejetJour[]; scans: RejetScan[]; scansTronques: boolean;
}

// ---- Vérification (3.2) ----
/** Server-side paginated result wrapper (mirrors backend PageDto). */
export interface PageResult<T> {
  content: T[]; page: number; size: number; totalElements: number; totalPages: number;
}
export type SearchField = 'codebarre' | 'numeroserie';
export type SearchMode = 'exact' | 'prefix';

/** One row of the Vérification Billet list. */
export interface BilletSearchRow {
  numeroserie: string; codebarre: string;
  activation: boolean; livre: boolean; vendu: boolean; utilise: boolean;
  eventTitle: string; modelName: string;
  dateVente: string | null; livreur: string | null; dateLivraison: string | null;
}
/** One row of the Vérification Voucher list. */
export interface VoucherSearchRow {
  eventTitle: string; modelName: string; numeroserie: string; codebarre: string;
  utilisation: boolean; vendu: boolean; activation: boolean; reservation: boolean;
  commande: string | null;
}
/** One access-log line (Public via tturnstile, VIP via vipaccess). */
export interface AccessLog {
  reference: number; codebarre: string;
  date: string | null; time: string | null;
  porte: string; granted: boolean;
}
/** Details modal payload (flags + identity + Public/VIP access logs). */
export interface TicketDetails {
  type: 'BILLET' | 'VOUCHER'; numeroserie: string; codebarre: string;
  eventTitle: string; ticketModel: string;
  vente: boolean; utilisation: boolean; reservation: boolean; activation: boolean;
  publicLog: AccessLog[]; vipLog: AccessLog[];
}

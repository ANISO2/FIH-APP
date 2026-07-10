import { Pipe, PipeTransform } from '@angular/core';

/** 18341 -> "18 341" (espace fine insécable française) */
@Pipe({ name: 'num', standalone: true })
export class NumPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR');
  }
}

/** 95.6 -> "95,6 %" */
@Pipe({ name: 'pct', standalone: true })
export class PctPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return `${value.toLocaleString('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`;
  }
}

/**
 * Parse a date string into a real Date, or null when there is effectively no
 * date. A missing date is stored/serialised as the Unix epoch (1970-01-01,
 * timestamp 0) and must never be shown as "1970" in the UI — so any date whose
 * year is 1970 or earlier is treated as "no date".
 */
export function realDate(value: string | null | undefined): Date | null {
  if (!value) return null;
  const d = new Date(value);
  if (isNaN(d.getTime())) return null;
  if (d.getFullYear() <= 1970) return null;   // hide Unix-epoch artefact dates
  return d;
}

/** "2025-07-29" -> "29 juil. 2025" (ou "29 juil." en version courte). 1970/epoch -> "—" */
@Pipe({ name: 'fdate', standalone: true })
export class FDatePipe implements PipeTransform {
  transform(value: string | null | undefined, short = false): string {
    const d = realDate(value);
    if (!d) return '—';
    const opts: Intl.DateTimeFormatOptions = short
      ? { day: '2-digit', month: 'short' }
      : { day: '2-digit', month: 'short', year: 'numeric' };
    return d.toLocaleDateString('fr-FR', opts);
  }
}

/**
 * Comme `fdate`, mais les entrées sans date réelle (placeholder époque 1970 =
 * type « FIH » général, non rattaché à un spectacle daté) affichent « Général »
 * au lieu de « — ». Réservé aux vues où ce type général peut apparaître
 * (Invitations & Badges, tourniquets, statistiques par événement).
 */
@Pipe({ name: 'gdate', standalone: true })
export class GDatePipe implements PipeTransform {
  transform(value: string | null | undefined, short = false): string {
    const d = realDate(value);
    if (!d) return 'Général';
    const opts: Intl.DateTimeFormatOptions = short
      ? { day: '2-digit', month: 'short' }
      : { day: '2-digit', month: 'short', year: 'numeric' };
    return d.toLocaleDateString('fr-FR', opts);
  }
}

/** 56720 -> "56 720,000 TND" (devise tunisienne, locale fr) */
@Pipe({ name: 'tnd', standalone: true })
export class TndPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    // Le dinar tunisien a 3 décimales (millimes). Intl gère le code "TND".
    return value.toLocaleString('fr-FR', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 3,
      maximumFractionDigits: 3
    });
  }
}

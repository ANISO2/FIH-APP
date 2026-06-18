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

/** "2025-07-29" -> "29 juil. 2025" (ou "29 juil." en version courte) */
@Pipe({ name: 'fdate', standalone: true })
export class FDatePipe implements PipeTransform {
  transform(value: string | null | undefined, short = false): string {
    if (!value) return '—';
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
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

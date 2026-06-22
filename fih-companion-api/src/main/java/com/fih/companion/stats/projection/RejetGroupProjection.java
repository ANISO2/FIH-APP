package com.fih.companion.stats.projection;

/** Generic (label, valeur) row — reused for rejets par catégorie and par porte. */
public interface RejetGroupProjection {
    String getLabel();
    long getValeur();
}

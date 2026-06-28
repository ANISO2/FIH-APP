package com.fih.companion.stats.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record RejetsDto(
        long totalRejets,
        long totalAcceptes,
        long totalScans,
        double tauxRejet,                 // % of scans that were refused
        List<Groupe> parCategorie,        // grouped from the description text
        List<Evenement> parEvenement,
        List<Groupe> parPorte,            // Public / VIP
        List<Modele> parModele,
        List<Jour> parJour,
        List<Scan> scans,                 // capped list for the table
        boolean scansTronques             // true if the list hit the cap
) {
    public record Groupe(String label, long valeur) {}
    public record Evenement(int eventId, String eventTitle, LocalDate eventDate, long rejets) {}
    public record Modele(int modelId, String modelName, long rejets) {}
    public record Jour(LocalDate jour, long rejets) {}
    public record Scan(String codebarre, String eventTitle, String porte, LocalDateTime dateTime, String description) {}
}

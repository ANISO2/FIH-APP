package com.fih.companion.diagnostics;

import com.fih.companion.access.AccessZoneResolver;
import com.fih.companion.domain.ModeleBillet;
import com.fih.companion.repository.ModeleBilletRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final JdbcTemplate jdbc;
    private final ModeleBilletRepository modeleBilletRepository;
    private final AccessZoneResolver accessZoneResolver;

    public DiagnosticsController(JdbcTemplate jdbc,
                                 ModeleBilletRepository modeleBilletRepository,
                                 AccessZoneResolver accessZoneResolver) {
        this.jdbc = jdbc;
        this.modeleBilletRepository = modeleBilletRepository;
        this.accessZoneResolver = accessZoneResolver;
    }

    @GetMapping("/modele/{reference}")
    public Map<String, Object> modele(@PathVariable Integer reference) {
        Map<String, Object> result = new LinkedHashMap<>();
        ModeleBillet model = modeleBilletRepository.findById(reference).orElse(null);
        if (model == null) {
            result.put("found", false);
            return result;
        }
        result.put("found", true);
        result.put("reference", model.getReference());
        result.put("modele", model.getModele());
        result.put("maxAccess", model.getMaxaccess());
        result.put("accessZones", accessZoneResolver.resolve(reference));
        result.put("accessBlobBytes", model.getAccess() == null ? 0 : model.getAccess().length);
        return result;
    }

    @GetMapping("/db")
    public Map<String, Object> dbDiagnostics() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1) Connection + identity
        try {
            String user = jdbc.queryForObject("SELECT current_user", String.class);
            result.put("connectionAlive", true);
            result.put("databaseUser", user);
        } catch (Exception ex) {
            result.put("connectionAlive", false);
            result.put("error", rootMessage(ex));
            return result;
        }

        // 2) A real read
        Integer count = jdbc.queryForObject("SELECT count(*) FROM evenement", Integer.class);
        result.put("evenementRowCount", count);

        // 3) Intentional write that MUST fail
        try {
            jdbc.update("INSERT INTO evenement(billet, ddate, titre, voucher, location) "
                    + "VALUES (false, CURRENT_DATE, '__diagnostic_never_persists__', false, 1)");
            // Reaching this line would mean the safety net failed.
            result.put("writeBlocked", false);
            result.put("writeAttempt",
                    "DANGER: INSERT succeeded. The read-only guarantee is broken — investigate the DB role.");
        } catch (Exception ex) {
            result.put("writeBlocked", true);
            result.put("writeAttempt", "INSERT correctly rejected: " + rootMessage(ex));
        }

        return result;
    }

     private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}

package com.fih.companion.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fih.companion.diagnostics.ConsoleLog;
import com.fih.companion.verification.dto.ExternalTicketVerify;
import com.fih.companion.verification.dto.TicketVerifyRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Calls the external ticket-verification web service (festivaldehammamet.com)
 * for the mobile "Info voucher" screen. Runs entirely server-side so the API
 * key never reaches the device.
 *
 * <p><b>product_id resolution.</b> The verify service keys tickets by ITS OWN
 * {@code product_id}, which does NOT equal our local {@code evenement}
 * reference. We resolve the real ids from {@link EventsCatalogGateway} (the
 * {@code /api/events} "en cours" list) and verify the scanned {@code qrcode}
 * against them.
 *
 * <p><b>Latency.</b> A festival can have ~20 ongoing products, and a ticket only
 * matches one. Probing them <em>sequentially</em> took ~0.5&nbsp;s each — up to
 * ~11&nbsp;s worst case — which tripped the mobile read-timeout ("délai
 * dépassé"). So the probes now run <em>concurrently</em> on a small shared pool
 * and we return the instant one product recognises the code; a successful
 * {@code code → product_id} is cached so a re-scan is a single call. Typical
 * latency is now one upstream round-trip (&lt;1&nbsp;s).
 *
 * <p>The gate access verdict ({@code /api/verify/voucher}) is NOT affected —
 * this only feeds the informational voucher screen. Every failure is caught and
 * turned into a French, non-blocking message; the raw HTTP status + body is
 * logged per product so different root causes stay distinguishable.
 */
@Service
public class TicketVerifyGateway {

    private static final String TAG = "TICKET-VERIFY";

    /** Cap on the response body written to the console, so a stray HTML page can't flood it. */
    private static final int LOG_BODY_MAX = 1200;

    /** Max concurrent product probes. Covers a full festival line-up in one wave. */
    private static final int PROBE_POOL = 20;

    private final TicketVerifyProperties props;
    private final EventsCatalogGateway eventsCatalog;
    private final ObjectMapper mapper;

    /** Built lazily on first real call so startup stays clean when disabled. */
    private volatile RestClient restClient;

    /** code → product_id that last verified, so a re-scan skips the probe. */
    private final Map<String, Integer> codeToProduct = new ConcurrentHashMap<>();

    /** Shared daemon pool for concurrent per-product probes. */
    private final ExecutorService probePool = Executors.newFixedThreadPool(PROBE_POOL, r -> {
        Thread t = new Thread(r, "ticket-verify-probe");
        t.setDaemon(true);
        return t;
    });

    public TicketVerifyGateway(TicketVerifyProperties props,
                               EventsCatalogGateway eventsCatalog,
                               ObjectMapper mapper) {
        this.props = props;
        this.eventsCatalog = eventsCatalog;
        this.mapper = mapper;
    }

    @PreDestroy
    void shutdown() {
        probePool.shutdownNow();
    }

    public VoucherInfoResponse fetch(String code) {
        if (!props.isEnabled()) {
            ConsoleLog.log(TAG, "DISABLED — fih.ticket-verify.base-url is blank, no call made.");
            return VoucherInfoResponse.pendingIntegration(code);
        }

        List<EventsCatalogGateway.Product> products = eventsCatalog.ongoingProducts();
        if (products.isEmpty()) {
            ConsoleLog.log(TAG, "ABORT — no ongoing product from /api/events; cannot resolve product_id for code='"
                    + code + "'.");
            return status("UNAVAILABLE", code,
                    "Catalogue des événements en cours indisponible. Réessayez.");
        }

        // Fast path: we already know which product this code lives in.
        Integer cached = codeToProduct.get(code);
        if (cached != null && containsId(products, cached)) {
            ConsoleLog.log(TAG, "cache hit — code='" + code + "' → product_id=" + cached + " (single call).");
            Attempt a = attempt(code, cached);
            if (a.matched() || a.hardFailure()) {
                return a.response();
            }
            // The remembered product no longer recognises it — drop and re-probe.
            codeToProduct.remove(code);
        }

        return probeAll(code, products);
    }

    // ----------------------------------------------------------- parallel probe

    private VoucherInfoResponse probeAll(String code, List<EventsCatalogGateway.Product> products) {
        ExecutorCompletionService<Attempt> ecs = new ExecutorCompletionService<>(probePool);
        List<Future<Attempt>> futures = new ArrayList<>(products.size());
        for (EventsCatalogGateway.Product p : products) {
            final Integer pid = p.id();
            futures.add(ecs.submit(() -> attempt(code, pid)));
        }

        VoucherInfoResponse anyClean = null; // service answered "pas ce produit"
        VoucherInfoResponse anyHard = null;  // transport / auth / format problem
        try {
            for (int i = 0; i < products.size(); i++) {
                Attempt a;
                try {
                    a = ecs.take().get();
                } catch (Exception probeError) {
                    // One probe threw unexpectedly; keep draining the rest.
                    continue;
                }
                if (a.matched()) {
                    codeToProduct.put(code, a.productId());
                    ConsoleLog.log(TAG, "<== MATCH code='" + code + "' → product_id=" + a.productId() + ".");
                    return a.response();
                }
                if (a.hardFailure()) {
                    anyHard = a.response();
                } else {
                    anyClean = a.response();
                }
            }
        } finally {
            for (Future<Attempt> f : futures) {
                f.cancel(true);
            }
        }

        ConsoleLog.log(TAG, "<== no match across " + products.size()
                + " ongoing product(s) for code='" + code + "'.");
        // If the service cleanly answered "not found" for at least one product it is
        // up and the code is simply invalid — prefer that over a transient error.
        if (anyClean != null) {
            return anyClean;
        }
        if (anyHard != null) {
            return anyHard;
        }
        return status("NOT_FOUND", code, "Billet introuvable pour les événements en cours.");
    }

    private static boolean containsId(List<EventsCatalogGateway.Product> products, Integer id) {
        for (EventsCatalogGateway.Product p : products) {
            if (id.equals(p.id())) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------- one attempt

    /** Outcome of a single verify call against one product_id. */
    private record Attempt(Integer productId, boolean matched, boolean hardFailure, VoucherInfoResponse response) {
        static Attempt matched(Integer pid, VoucherInfoResponse r)  { return new Attempt(pid, true, false, r); }
        static Attempt notFound(Integer pid, VoucherInfoResponse r) { return new Attempt(pid, false, false, r); }
        static Attempt hard(Integer pid, VoucherInfoResponse r)     { return new Attempt(pid, false, true, r); }
    }

    private Attempt attempt(String code, Integer productId) {
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(new TicketVerifyRequest(code, productId));
        } catch (Exception serialiseError) {
            ConsoleLog.error(TAG, "cannot serialise request for code='" + code + "'", serialiseError);
            return Attempt.hard(productId, status("UNAVAILABLE", code, "Erreur interne de préparation de la requête."));
        }

        ConsoleLog.log(TAG, "==> POST " + props.getBaseUrl() + " · " + requestBody);

        ResponseEntity<String> res;
        try {
            res = client().post()
                    .uri(props.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                            h.set("X-Api-Key", props.getApiKey());
                        }
                    })
                    .body(requestBody)
                    .retrieve()
                    // Never throw on status: we want to LOG the raw body of a 4xx/5xx.
                    .onStatus(s -> true, (rq, rs) -> { })
                    .toEntity(String.class);
        } catch (Exception transportError) {
            ConsoleLog.error(TAG, "<== TRANSPORT FAILED for code='" + code + "' (product_id=" + productId + ") — "
                    + transportError.getClass().getSimpleName() + ": " + transportError.getMessage(), transportError);
            return Attempt.hard(productId, status("UNAVAILABLE", code,
                    "Service externe injoignable depuis le serveur (" + transportError.getClass().getSimpleName()
                            + "). Vérifiez la connexion / le pare-feu."));
        }

        int http = res.getStatusCode().value();
        String raw = res.getBody();
        ConsoleLog.log(TAG, "<== HTTP " + http + " (product_id=" + productId + ")"
                + " · Content-Type=" + res.getHeaders().getContentType()
                + " · body=" + abbreviate(raw));

        if (http == 404) {
            return Attempt.notFound(productId, status("NOT_FOUND", code, "Billet introuvable côté service externe."));
        }
        if (http == 401 || http == 403) {
            return Attempt.hard(productId, status("UNAVAILABLE", code,
                    "Clé API refusée par le service externe (HTTP " + http + ")."));
        }
        if (http >= 300) {
            return Attempt.hard(productId, status("UNAVAILABLE", code, "Service externe : HTTP " + http + "."));
        }
        if (raw == null || raw.isBlank()) {
            return Attempt.hard(productId, status("UNAVAILABLE", code, "Réponse vide du service externe."));
        }

        ExternalTicketVerify ext;
        try {
            ext = mapper.readValue(raw, ExternalTicketVerify.class);
        } catch (Exception parseError) {
            ConsoleLog.error(TAG, "<== body is not the expected JSON for code='" + code
                    + "' (product_id=" + productId + ") — " + parseError.getMessage(), parseError);
            return Attempt.hard(productId, status("UNAVAILABLE", code,
                    "Réponse inattendue du service externe (format). Voir les logs du serveur."));
        }

        VoucherInfoResponse mapped = toResponse(ext, code);
        // error:false => the ticket lives in this product (a real match).
        // error:true  => "pas dans ce produit" => not a match, let the probe continue.
        return ext.error()
                ? Attempt.notFound(productId, mapped)
                : Attempt.matched(productId, mapped);
    }

    // ------------------------------------------------------------------ mapping

    private VoucherInfoResponse toResponse(ExternalTicketVerify ext, String code) {
        if (ext == null) {
            return status("UNAVAILABLE", code, "Réponse vide du service externe. Réessayez.");
        }
        ExternalTicketVerify.Data d = ext.data();
        String status = ext.error() ? "ERROR" : "OK";
        String message = (ext.message() == null || ext.message().isBlank())
                ? (ext.error() ? "Billet refusé par le service externe." : null)
                : ext.message();

        ConsoleLog.log(TAG, "<== mapped status=" + status
                + " · used=" + (d == null ? null : d.used())
                + " · holder=" + (d == null ? null : (d.prenom() + " " + d.nom()))
                + " · message=" + message);

        return new VoucherInfoResponse(
                status,
                "EXTERNAL_SERVICE",
                code,
                null, null, null, null, null, null, null, null, null,
                message,
                d == null ? null : d.used(),
                d == null ? null : d.usedDate(),
                d == null ? null : d.ticket(),
                d == null ? null : d.ticketCin(),
                d == null ? null : d.prenom(),
                d == null ? null : d.nom());
    }

    private VoucherInfoResponse status(String status, String code, String message) {
        return new VoucherInfoResponse(
                status, "EXTERNAL_SERVICE", code,
                null, null, null, null, null, null, null, null, null,
                message,
                null, null, null, null, null, null);
    }

    /** Collapse whitespace and cap length so one log line stays one log line. */
    private static String abbreviate(String s) {
        if (s == null) {
            return "<null>";
        }
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= LOG_BODY_MAX
                ? flat
                : flat.substring(0, LOG_BODY_MAX) + "…[" + flat.length() + " chars total]";
    }

    // ------------------------------------------------------------- http client
    private RestClient client() {
        RestClient local = this.restClient;
        if (local == null) {
            synchronized (this) {
                local = this.restClient;
                if (local == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
                    factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
                    local = RestClient.builder().requestFactory(factory).build();
                    this.restClient = local;
                }
            }
        }
        return local;
    }
}

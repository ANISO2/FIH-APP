package com.fih.companion.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fih.companion.diagnostics.ConsoleLog;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Reads the ongoing ("en cours") product catalogue from the festival's
 * {@code GET /api/events} endpoint. The ticket-verify service keys tickets by
 * ITS OWN {@code product_id}, which does NOT equal our local {@code evenement}
 * reference — sending the local id made every scan look like an unknown ticket.
 * This gateway resolves the real, currently-valid product ids so the verify call
 * can be issued against the right one.
 *
 * <p>The list is cached for {@code fih.ticket-verify.events-cache-ttl-ms} because
 * the ongoing set does not change between scans. Every failure is swallowed and
 * logged (this only feeds the informational voucher screen), returning the last
 * good catalogue when a refresh transiently fails, or an empty list otherwise.
 *
 * <p>Parsing is deliberately tolerant: the payload may be a bare JSON array or an
 * envelope {@code {"error":..,"data":[...]}}, and the id/title/date keys may vary
 * slightly upstream. The raw body is logged on every call so any shape surprise
 * is diagnosable in one scan.
 */
@Service
public class EventsCatalogGateway {

    private static final String TAG = "EVENTS-CATALOG";
    private static final int LOG_BODY_MAX = 1500;

    private final TicketVerifyProperties props;
    private final ObjectMapper mapper;
    private final long ttlMs;

    private volatile RestClient restClient;
    private volatile List<Product> cache = List.of();
    private volatile long cachedAt = 0L;

    public EventsCatalogGateway(TicketVerifyProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.ttlMs = Math.max(0L, props.getEventsCacheTtlMs());
    }

    /** One ongoing product from {@code /api/events}. */
    public record Product(Integer id, String title, String date) {
    }

    /** Ongoing products, cached for the configured TTL. Never throws. */
    public List<Product> ongoingProducts() {
        long now = System.currentTimeMillis();
        List<Product> local = cache;
        if (!local.isEmpty() && (now - cachedAt) < ttlMs) {
            return local;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (!cache.isEmpty() && (now - cachedAt) < ttlMs) {
                return cache;
            }
            List<Product> fresh = fetch();
            if (!fresh.isEmpty()) {
                cache = fresh;
                cachedAt = now;
            } else if (!cache.isEmpty()) {
                ConsoleLog.log(TAG, "refresh returned nothing — keeping " + cache.size() + " cached product(s).");
            }
            return cache;
        }
    }

    /** Force a refresh on the next call (e.g. after a full verify miss). */
    public void invalidate() {
        cachedAt = 0L;
    }

    // ---------------------------------------------------------------- fetch

    private List<Product> fetch() {
        String url = props.getEventsUrl();
        if (url == null || url.isBlank()) {
            ConsoleLog.log(TAG, "DISABLED — no events-url resolved; cannot list ongoing products.");
            return List.of();
        }

        ConsoleLog.log(TAG, "==> GET " + url);

        ResponseEntity<String> res;
        try {
            res = client().get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                            h.set("X-Api-Key", props.getApiKey());
                        }
                    })
                    .retrieve()
                    .onStatus(s -> true, (rq, rs) -> { })
                    .toEntity(String.class);
        } catch (Exception transportError) {
            ConsoleLog.error(TAG, "<== TRANSPORT FAILED — "
                    + transportError.getClass().getSimpleName() + ": " + transportError.getMessage(), transportError);
            return List.of();
        }

        int http = res.getStatusCode().value();
        String raw = res.getBody();
        ConsoleLog.log(TAG, "<== HTTP " + http
                + " · Content-Type=" + res.getHeaders().getContentType()
                + " · body=" + abbreviate(raw));

        if (http >= 300 || raw == null || raw.isBlank()) {
            return List.of();
        }
        return parse(raw);
    }

    private List<Product> parse(String raw) {
        List<Product> out = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(raw);
            JsonNode arr;
            if (root.isArray()) {
                arr = root;
            } else if (root.path("data").isArray()) {
                arr = root.path("data");
            } else if (root.path("events").isArray()) {
                arr = root.path("events");
            } else {
                arr = mapper.createArrayNode();
            }
            for (JsonNode n : arr) {
                Integer id = intField(n, "product_id", "productId", "id", "reference", "evenement");
                if (id == null) {
                    continue;
                }
                String title = textField(n, "title", "titre", "name", "nom", "libelle");
                String date = textField(n, "date", "ddate", "date_debut", "dateDebut", "start");
                out.add(new Product(id, title, date));
            }
        } catch (Exception parseError) {
            ConsoleLog.error(TAG, "<== events body is not valid JSON — " + parseError.getMessage(), parseError);
            return List.of();
        }
        ConsoleLog.log(TAG, "parsed " + out.size() + " ongoing product(s): " + out);
        return Collections.unmodifiableList(out);
    }

    // ---------------------------------------------------------------- helpers

    private static Integer intField(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v == null || v.isNull()) {
                continue;
            }
            if (v.isNumber()) {
                return v.asInt();
            }
            if (v.isTextual()) {
                try {
                    return Integer.parseInt(v.asText().trim());
                } catch (NumberFormatException ignored) {
                    // not an int under this key — try the next
                }
            }
        }
        return null;
    }

    private static String textField(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "<null>";
        }
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= LOG_BODY_MAX
                ? flat
                : flat.substring(0, LOG_BODY_MAX) + "…[" + flat.length() + " chars total]";
    }

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

package com.fih.companion.verification;

import com.fih.companion.verification.dto.ExternalVoucherInfo;
import com.fih.companion.verification.dto.VoucherInfoRequest;
import com.fih.companion.verification.dto.VoucherInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;


@Primary
@Component
public class ConfigDrivenVoucherVerificationGateway implements VoucherVerificationGateway {

    private static final Logger log =
            LoggerFactory.getLogger(ConfigDrivenVoucherVerificationGateway.class);

    private final VoucherServiceProperties props;
    private final StubVoucherVerificationGateway stub;

    /** Built lazily on first real call so startup stays clean when the feature is off. */
    private volatile RestClient restClient;

    public ConfigDrivenVoucherVerificationGateway(VoucherServiceProperties props,
                                                  StubVoucherVerificationGateway stub) {
        this.props = props;
        this.stub = stub;
    }

    @Override
    public VoucherInfoResponse fetch(VoucherInfoRequest request) {
        String code = request == null ? null : request.code();

        // ---- Feature OFF: identical to today's behaviour -----------------------
        if (!props.isEnabled()) {
            return stub.fetch(request);
        }

        // ---- Feature ON: call the external service, never let it blow up --------
        try {
            String url = UriComponentsBuilder
                    .fromUriString(props.getBaseUrl())
                    .queryParam("code", code)
                    .encode()
                    .toUriString();

            ExternalVoucherInfo ext = client().get()
                    .uri(url)
                    .headers(h -> {
                        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                            h.set("X-Api-Key", props.getApiKey());
                        }
                    })
                    .retrieve()
                    .body(ExternalVoucherInfo.class);

            return toResponse(ext, code);

        } catch (RestClientResponseException httpError) {
            // The external service answered, but with a non-2xx status.
            if (httpError.getStatusCode().value() == 404) {
                return notFound(code);
            }
            log.warn("Voucher-info external HTTP {} for code {}",
                    httpError.getStatusCode().value(), code);
            return safeFallback(code);

        } catch (Exception anyError) {
            // Timeout, connection refused, DNS, parse error... anything at all.
            log.warn("Voucher-info external call failed for code {}: {}",
                    code, anyError.getMessage());
            return safeFallback(code);
        }
    }

    // ------------------------------------------------------------------ mapping

    private VoucherInfoResponse toResponse(ExternalVoucherInfo ext, String requestedCode) {
        if (ext == null) {
            return safeFallback(requestedCode);
        }
        String status = (ext.status() == null || ext.status().isBlank()) ? "OK" : ext.status();
        return new VoucherInfoResponse(
                status,
                "EXTERNAL_SERVICE",                                   // source is always us delegating
                ext.code() != null ? ext.code() : requestedCode,     // echo the scanned code
                ext.numeroserie(),
                ext.codebarre(),
                ext.eventTitle(),
                ext.eventDate(),
                ext.model(),
                ext.prix(),
                ext.vendu(),
                ext.dateVente(),
                ext.accessCounter(),
                ext.message());
    }

    private VoucherInfoResponse notFound(String code) {
        return new VoucherInfoResponse(
                "NOT_FOUND", "EXTERNAL_SERVICE", code,
                null, null, null, null, null, null, null, null, null,
                "Voucher introuvable côté service externe.");
    }


    private VoucherInfoResponse safeFallback(String code) {
        return new VoucherInfoResponse(
                "PENDING_INTEGRATION", "EXTERNAL_SERVICE", code,
                null, null, null, null, null, null, null, null, null,
                "Service de vérification voucher momentanément indisponible. Réessayez.");
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

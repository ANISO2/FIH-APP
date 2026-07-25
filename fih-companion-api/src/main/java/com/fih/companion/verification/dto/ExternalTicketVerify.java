package com.fih.companion.verification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;


/**
 * Raw response from POST {base-url} (festivaldehammamet.com/ticket/verify):
 * <pre>
 * { "error": bool, "message": string,
 *   "data": { "used": bool, "used_date": string, "ticket": string,
 *             "ticket_cin": string, "prenom": string, "nom": string } }
 * </pre>
 * Unknown / extra fields are ignored so a contract tweak upstream won't 500 us.
 *
 * <p><b>Array-vs-object quirk.</b> The upstream service is PHP-flavoured: on the
 * happy path {@code data} is a JSON <em>object</em>, but on the error / not-found
 * path (e.g. {@code {"error":true,"message":"Aucun ticket trouvé ...","data":[]}})
 * an empty associative array serialises as {@code []} — a JSON <em>array</em>.
 * A plain object mapping then blows up with a {@code MismatchedInputException}
 * (START_ARRAY where an object is expected), which was being swallowed as a
 * generic "format" error and hid the real message from the phone. The custom
 * deserialiser below treats any array (empty or not) as "no data" and parses a
 * real object normally, so the upstream {@code message} always reaches the user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalTicketVerify(
        boolean error,
        String message,
        @JsonDeserialize(using = Data.ArrayTolerantDeserializer.class)
        Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Boolean used,
            @JsonProperty("used_date") String usedDate,
            String ticket,
            @JsonProperty("ticket_cin") String ticketCin,
            String prenom,
            String nom
    ) {

        /**
         * Deserialises {@code data} that may arrive either as an object (real
         * payload) or as an array (PHP's empty {@code []} on the error path).
         * Any array — or an explicit null — becomes {@code null}; an object is
         * parsed with the default record deserialiser (no recursion, because the
         * custom deserialiser is bound to the field, not to {@code Data} itself).
         */
        public static final class ArrayTolerantDeserializer extends JsonDeserializer<Data> {
            @Override
            public Data deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonToken t = p.currentToken();
                if (t == JsonToken.START_ARRAY) {
                    p.skipChildren(); // consume the whole (usually empty) array
                    return null;
                }
                if (t == JsonToken.VALUE_NULL) {
                    return null;
                }
                return ctxt.readValue(p, Data.class);
            }
        }
    }
}

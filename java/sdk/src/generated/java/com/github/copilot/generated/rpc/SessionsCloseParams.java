/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session ID to close.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionsCloseParams.$NullableDeserializer.class)
public record SessionsCloseParams(
    /** Session ID to close */
    @JsonProperty("sessionId") String sessionId,
    /** Run deferred session-end hooks with their full configured timeout when the host stays alive. */
    @JsonProperty("detachSessionEndHooks") Boolean detachSessionEndHooks,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsCloseParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsCloseParams(String sessionId, Boolean detachSessionEndHooks) {
        this(sessionId, detachSessionEndHooks, java.util.Set.of());
    }

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private Boolean detachSessionEndHooks;
        private boolean $presence1;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code detachSessionEndHooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder detachSessionEndHooks(Boolean value) {
            this.detachSessionEndHooks = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsCloseParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && detachSessionEndHooks == null) $explicitNulls.add("detachSessionEndHooks");
            return new SessionsCloseParams(sessionId, detachSessionEndHooks, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("detachSessionEndHooks") && detachSessionEndHooks == null) values.put("detachSessionEndHooks", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionsCloseParams> {
        @Override
        public SessionsCloseParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionsCloseParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("detachSessionEndHooks") && tree.get("detachSessionEndHooks").isNull()) $explicitNulls.add("detachSessionEndHooks");
            return new SessionsCloseParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "detachSessionEndHooks", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Creates session-close parameters with the default session-end hook behavior.
     * @deprecated Use named construction via {@link #builder()}.
     * @param sessionId Session ID to close
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsCloseParams(
        String sessionId
    ) {
        this(sessionId, null);
    }
}

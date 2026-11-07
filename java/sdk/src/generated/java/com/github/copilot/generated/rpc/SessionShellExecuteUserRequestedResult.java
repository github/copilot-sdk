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
 * Result of a user-requested shell command.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionShellExecuteUserRequestedResult.$NullableDeserializer.class)
public record SessionShellExecuteUserRequestedResult(
    /** Tool call id emitted for the shell execution */
    @JsonProperty("toolCallId") String toolCallId,
    /** Whether the command completed successfully */
    @JsonProperty("success") Boolean success,
    /** Captured command output */
    @JsonProperty("output") String output,
    /** Process exit code, when available */
    @JsonProperty("exitCode") Long exitCode,
    /** Error output when the execution failed */
    @JsonProperty("error") String error,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionShellExecuteUserRequestedResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionShellExecuteUserRequestedResult(String toolCallId, Boolean success, String output, Long exitCode, String error) {
        this(toolCallId, success, output, exitCode, error, java.util.Set.of());
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
        private String toolCallId;
        private Boolean success;
        private String output;
        private Long exitCode;
        private boolean $presence3;
        private String error;

        /**
         * Sets the {@code toolCallId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolCallId(String value) {
            this.toolCallId = value;
            return this;
        }

        /**
         * Sets the {@code success} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder success(Boolean value) {
            this.success = value;
            return this;
        }

        /**
         * Sets the {@code output} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder output(String value) {
            this.output = value;
            return this;
        }

        /**
         * Sets the {@code exitCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder exitCode(Long value) {
            this.exitCode = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionShellExecuteUserRequestedResult build() {
            if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
            if (success == null) throw new IllegalStateException("Missing required property: success");
            if (output == null) throw new IllegalStateException("Missing required property: output");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && exitCode == null) $explicitNulls.add("exitCode");
            return new SessionShellExecuteUserRequestedResult(toolCallId, success, output, exitCode, error, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("exitCode") && exitCode == null) values.put("exitCode", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionShellExecuteUserRequestedResult> {
        @Override
        public SessionShellExecuteUserRequestedResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionShellExecuteUserRequestedResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("exitCode") && tree.get("exitCode").isNull()) $explicitNulls.add("exitCode");
            return new SessionShellExecuteUserRequestedResult(readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "success", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "output", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "exitCode", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}

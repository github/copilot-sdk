/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The settings a consumer should act on inside a repository, together with any diagnostic raised while reading the repository layer.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = UserSettingsLoadAndMergeRepoResult.$NullableDeserializer.class)
record UserSettingsLoadAndMergeRepoResult(
    /** The merged settings document, keyed by setting name. Equals the supplied user layer when the repository declares no settings. */
    @JsonProperty("settings") Map<String, Object> settings,
    /** Why part of the repository document was ignored, as a single human-readable string joining every diagnostic with `; `. Null when it loaded cleanly. The settings that did load are still merged, so a consumer should surface this and continue rather than treat it as a failure. */
    @JsonProperty("warning") String warning,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsLoadAndMergeRepoResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsLoadAndMergeRepoResult(Map<String, Object> settings, String warning) {
        this(settings, warning, java.util.Set.of());
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
        private Map<String, Object> settings;
        private String warning;
        private boolean $presence1;

        /**
         * Sets the {@code settings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settings(Map<String, Object> value) {
            this.settings = value;
            return this;
        }

        /**
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warning(String value) {
            this.warning = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsLoadAndMergeRepoResult build() {
            if (settings == null) throw new IllegalStateException("Missing required property: settings");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && warning == null) $explicitNulls.add("warning");
            return new UserSettingsLoadAndMergeRepoResult(settings, warning, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("warning") && warning == null) values.put("warning", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<UserSettingsLoadAndMergeRepoResult> {
        @Override
        public UserSettingsLoadAndMergeRepoResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(UserSettingsLoadAndMergeRepoResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("warning") && tree.get("warning").isNull()) $explicitNulls.add("warning");
            return new UserSettingsLoadAndMergeRepoResult(readProperty(context, tree, "settings", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "warning", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}

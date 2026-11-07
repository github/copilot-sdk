/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * On-disk locations of the settings files this runtime reads. A path is reported whether or not the file exists.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = UserSettingsGetPathsResult.$NullableDeserializer.class)
record UserSettingsGetPathsResult(
    /** The user settings file: `settings.json` under the resolved Copilot home. This is the file `user.settings.get`, `user.settings.load`, and `user.settings.set` read and write. */
    @JsonProperty("userSettings") String userSettings,
    /** The repository's checked-in settings file, or null when no repository root was supplied. */
    @JsonProperty("repoSettings") String repoSettings,
    /** The repository's local, untracked settings file, or null when no repository root was supplied. */
    @JsonProperty("repoLocalSettings") String repoLocalSettings,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsGetPathsResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsGetPathsResult(String userSettings, String repoSettings, String repoLocalSettings) {
        this(userSettings, repoSettings, repoLocalSettings, java.util.Set.of());
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
        private String userSettings;
        private String repoSettings;
        private boolean $presence1;
        private String repoLocalSettings;
        private boolean $presence2;

        /**
         * Sets the {@code userSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userSettings(String value) {
            this.userSettings = value;
            return this;
        }

        /**
         * Sets the {@code repoSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repoSettings(String value) {
            this.repoSettings = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code repoLocalSettings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repoLocalSettings(String value) {
            this.repoLocalSettings = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsGetPathsResult build() {
            if (userSettings == null) throw new IllegalStateException("Missing required property: userSettings");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence1 && repoSettings == null) $explicitNulls.add("repoSettings");
            if ($presence2 && repoLocalSettings == null) $explicitNulls.add("repoLocalSettings");
            return new UserSettingsGetPathsResult(userSettings, repoSettings, repoLocalSettings, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("repoSettings") && repoSettings == null) values.put("repoSettings", null);
        if ($explicitNulls.contains("repoLocalSettings") && repoLocalSettings == null) values.put("repoLocalSettings", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<UserSettingsGetPathsResult> {
        @Override
        public UserSettingsGetPathsResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(UserSettingsGetPathsResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("repoSettings") && tree.get("repoSettings").isNull()) $explicitNulls.add("repoSettings");
            if (tree.has("repoLocalSettings") && tree.get("repoLocalSettings").isNull()) $explicitNulls.add("repoLocalSettings");
            return new UserSettingsGetPathsResult(readProperty(context, tree, "userSettings", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "repoSettings", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "repoLocalSettings", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}

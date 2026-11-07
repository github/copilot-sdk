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
 * Installed plugin record for a session, with marketplace, version, install time, enabled state, cache path, and source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionInstalledPlugin.$NullableDeserializer.class)
public record SessionInstalledPlugin(
    /** Plugin name */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from (empty string for direct repo installs) */
    @JsonProperty("marketplace") String marketplace,
    /** Installed version, if known */
    @JsonProperty("version") String version,
    /** Installation timestamp (ISO-8601) */
    @JsonProperty("installed_at") String installedAt,
    /** Whether the plugin is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Path where the plugin is cached locally */
    @JsonProperty("cache_path") String cachePath,
    /** Source descriptor for direct repo installs (when marketplace is empty) */
    @JsonProperty("source") Object source,
    /** Installation-time marketplace or direct source, independent of mutable marketplace aliases. Local paths are absolute. Absent for older installs; reinstall to record provenance. */
    @JsonProperty("installed_source") Object installedSource,
    /** Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs. */
    @JsonProperty("source_sha") String sourceSha,
    /** Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key. */
    @JsonProperty("installed_from") String installedFrom,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionInstalledPlugin { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionInstalledPlugin(String name, String marketplace, String version, String installedAt, Boolean enabled, String cachePath, Object source, Object installedSource, String sourceSha, String installedFrom) {
        this(name, marketplace, version, installedAt, enabled, cachePath, source, installedSource, sourceSha, installedFrom, java.util.Set.of());
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
        private String name;
        private String marketplace;
        private String version;
        private String installedAt;
        private Boolean enabled;
        private String cachePath;
        private Object source;
        private boolean $presence6;
        private Object installedSource;
        private boolean $presence7;
        private String sourceSha;
        private String installedFrom;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code marketplace} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder marketplace(String value) {
            this.marketplace = value;
            return this;
        }

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code installed_at} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedAt(String value) {
            this.installedAt = value;
            return this;
        }

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Sets the {@code cache_path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cachePath(String value) {
            this.cachePath = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(Object value) {
            this.source = value;
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code installed_source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedSource(Object value) {
            this.installedSource = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code source_sha} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourceSha(String value) {
            this.sourceSha = value;
            return this;
        }

        /**
         * Sets the {@code installed_from} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedFrom(String value) {
            this.installedFrom = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionInstalledPlugin build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (marketplace == null) throw new IllegalStateException("Missing required property: marketplace");
            if (installedAt == null) throw new IllegalStateException("Missing required property: installed_at");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence6 && source == null) $explicitNulls.add("source");
            if ($presence7 && installedSource == null) $explicitNulls.add("installed_source");
            return new SessionInstalledPlugin(name, marketplace, version, installedAt, enabled, cachePath, source, installedSource, sourceSha, installedFrom, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("source") && source == null) values.put("source", null);
        if ($explicitNulls.contains("installed_source") && installedSource == null) values.put("installed_source", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionInstalledPlugin> {
        @Override
        public SessionInstalledPlugin deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionInstalledPlugin.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("source") && tree.get("source").isNull()) $explicitNulls.add("source");
            if (tree.has("installed_source") && tree.get("installed_source").isNull()) $explicitNulls.add("installed_source");
            return new SessionInstalledPlugin(readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "marketplace", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "version", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "installed_at", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "cache_path", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "installed_source", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "source_sha", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "installed_from", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Plugin name
     * @param marketplace Marketplace the plugin came from (empty string for direct repo installs)
     * @param version Installed version, if known
     * @param installedAt Installation timestamp (ISO-8601)
     * @param enabled Whether the plugin is currently enabled
     * @param cachePath Path where the plugin is cached locally
     * @param source Source descriptor for direct repo installs (when marketplace is empty)
     * @param sourceSha Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs.
     * @param installedFrom Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionInstalledPlugin(
        String name,
        String marketplace,
        String version,
        String installedAt,
        Boolean enabled,
        String cachePath,
        Object source,
        String sourceSha,
        String installedFrom
    ) {
        this(name, marketplace, version, installedAt, enabled, cachePath, source, null, sourceSha, installedFrom);
    }
}

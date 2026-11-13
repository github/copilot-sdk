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
 * A Team marketplace fallback. Explicit marketplace configuration takes precedence.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamPluginMarketplace(
    /** Validated GitHub repository source for the marketplace. */
    @JsonProperty("source") TeamPluginMarketplaceSource source
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TeamPluginMarketplaceSource.$NullableDeserializer.class)
    public record TeamPluginMarketplaceSource(
        /** Marketplace source discriminator. Currently only github is supported. */
        @JsonProperty("source") String source,
        /** GitHub repository in owner/name form. */
        @JsonProperty("repo") String repo,
        /** Branch or tag the marketplace is read from. Absent means the default branch. */
        @JsonProperty("ref") String ref,
        /** Folder in the repository that holds the marketplace. Absent means the repository root. */
        @JsonProperty("path") String path,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public TeamPluginMarketplaceSource { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public TeamPluginMarketplaceSource(String source, String repo, String ref, String path) {
            this(source, repo, ref, path, java.util.Set.of());
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
            private String source;
            private String repo;
            private String ref;
            private boolean $presence2;
            private String path;
            private boolean $presence3;

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(String value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code repo} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder repo(String value) {
                this.repo = value;
                return this;
            }

            /**
             * Sets the {@code ref} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder ref(String value) {
                this.ref = value;
                this.$presence2 = true;
                return this;
            }

            /**
             * Sets the {@code path} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder path(String value) {
                this.path = value;
                this.$presence3 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public TeamPluginMarketplaceSource build() {
                if (source == null) throw new IllegalStateException("Missing required property: source");
                if (repo == null) throw new IllegalStateException("Missing required property: repo");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence2 && ref == null) $explicitNulls.add("ref");
                if ($presence3 && path == null) $explicitNulls.add("path");
                return new TeamPluginMarketplaceSource(source, repo, ref, path, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("ref") && ref == null) values.put("ref", null);
            if ($explicitNulls.contains("path") && path == null) values.put("path", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<TeamPluginMarketplaceSource> {
            @Override
            public TeamPluginMarketplaceSource deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(TeamPluginMarketplaceSource.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("ref") && tree.get("ref").isNull()) $explicitNulls.add("ref");
                if (tree.has("path") && tree.get("path").isNull()) $explicitNulls.add("path");
                return new TeamPluginMarketplaceSource(readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "repo", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "ref", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "path", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
            }

            private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JsonNode tree, String name,
                    com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
                if (!tree.has(name)) return null;
                return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
            }
        }

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param source Marketplace source discriminator. Currently only github is supported.
         * @param repo GitHub repository in owner/name form.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public TeamPluginMarketplaceSource(
            String source,
            String repo
        ) {
            this(source, repo, null, null);
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TeamPluginMarketplace {}

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
        private TeamPluginMarketplaceSource source;

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(TeamPluginMarketplaceSource value) {
            this.source = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TeamPluginMarketplace build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            return new TeamPluginMarketplace(source);
        }
    }
}

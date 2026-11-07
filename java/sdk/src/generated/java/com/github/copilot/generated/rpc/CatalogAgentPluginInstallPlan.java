/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * What installing an Agent Plugin would do, for the user to review and confirm. Provisional: the catalogue install data it is built from is not final.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogAgentPluginInstallPlan(
    /** Every valid install source, in preference order: materialization, then marketplace, then repository. Each kind appears at most once. */
    @JsonProperty("sources") List<CatalogAgentPluginInstallSource> sources,
    /** The plugin's name, as named in its marketplace or manifest. Omitted from a materialisation-only plan, whose sources are only `materialization`: the verified manifests supply the name in the plugins.planInstall review. */
    @JsonProperty("pluginName") String pluginName,
    /** Exact plugin version to install. A plan with a marketplace source carries at least one of version and commit. */
    @JsonProperty("version") String version,
    /** Exact full commit ID to install, in lowercase hex. Always present when the plan has a repository source; a plan with a marketplace source carries at least one of version and commit. */
    @JsonProperty("commit") String commit,
    /** Components the plugin contributes, in canonical order. Empty only in a materialisation-only plan, where the verified manifests list them in the plugins.planInstall review. */
    @JsonProperty("components") List<CatalogAgentPluginComponent> components,
    /** Canvas IDs the plugin contributes. Non-empty exactly when components include `canvas`. */
    @JsonProperty("canvasIds") List<String> canvasIds,
    /** Extension IDs the plugin contributes. Non-empty exactly when components include `extension`. */
    @JsonProperty("extensionIds") List<String> extensionIds,
    /** MCP servers the plugin depends on but does not bundle. */
    @JsonProperty("mcpDependencies") List<String> mcpDependencies,
    /** Connectors the plugin depends on. */
    @JsonProperty("connectorDependencies") List<String> connectorDependencies
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CatalogAgentPluginInstallPlan {}

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
        private List<CatalogAgentPluginInstallSource> sources;
        private String pluginName;
        private String version;
        private String commit;
        private List<CatalogAgentPluginComponent> components;
        private List<String> canvasIds;
        private List<String> extensionIds;
        private List<String> mcpDependencies;
        private List<String> connectorDependencies;

        /**
         * Sets the {@code sources} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sources(List<CatalogAgentPluginInstallSource> value) {
            this.sources = value;
            return this;
        }

        /**
         * Sets the {@code pluginName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginName(String value) {
            this.pluginName = value;
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
         * Sets the {@code commit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder commit(String value) {
            this.commit = value;
            return this;
        }

        /**
         * Sets the {@code components} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder components(List<CatalogAgentPluginComponent> value) {
            this.components = value;
            return this;
        }

        /**
         * Sets the {@code canvasIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canvasIds(List<String> value) {
            this.canvasIds = value;
            return this;
        }

        /**
         * Sets the {@code extensionIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder extensionIds(List<String> value) {
            this.extensionIds = value;
            return this;
        }

        /**
         * Sets the {@code mcpDependencies} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpDependencies(List<String> value) {
            this.mcpDependencies = value;
            return this;
        }

        /**
         * Sets the {@code connectorDependencies} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder connectorDependencies(List<String> value) {
            this.connectorDependencies = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogAgentPluginInstallPlan build() {
            if (sources == null) throw new IllegalStateException("Missing required property: sources");
            if (components == null) throw new IllegalStateException("Missing required property: components");
            if (canvasIds == null) throw new IllegalStateException("Missing required property: canvasIds");
            if (extensionIds == null) throw new IllegalStateException("Missing required property: extensionIds");
            if (mcpDependencies == null) throw new IllegalStateException("Missing required property: mcpDependencies");
            if (connectorDependencies == null) throw new IllegalStateException("Missing required property: connectorDependencies");
            return new CatalogAgentPluginInstallPlan(sources, pluginName, version, commit, components, canvasIds, extensionIds, mcpDependencies, connectorDependencies);
        }
    }
}

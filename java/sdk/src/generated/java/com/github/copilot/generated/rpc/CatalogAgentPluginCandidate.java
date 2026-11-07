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
 * An inert Agent Plugin catalog result. Its canonical catalog identity, declared version, repository source claim, and explicit compatibility tags are safe to correlate, while its descriptor, URL, raw data, materialisation reference, and installed-plugin state remain runtime-private. A handle is minted only under agent-plugin-confirmed-installation, for a candidate with a complete materialisation reference.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogAgentPluginCandidate extends CatalogCandidate {

    @JsonProperty("kind")
    private final String kind = "plugin";

    @Override
    public String getKind() { return kind; }

    /** Media type naming the plugin's format. */
    @JsonProperty("mediaType")
    private CatalogAgentPluginMediaType mediaType;

    /** Validated, normalised catalogue resource URN. This identity comes only from the catalog identifier and is never inferred from display text or installed-plugin state. */
    @JsonProperty("identity")
    private String identity;

    /** Optional version declared by the catalog source. Omitted rather than guessed when the source supplies no version. */
    @JsonProperty("version")
    private String version;

    /** Display name taken verbatim from the card. Inert untrusted text. */
    @JsonProperty("displayName")
    private String displayName;

    /** Description taken verbatim from the card. Inert untrusted text. */
    @JsonProperty("description")
    private String description;

    /** Publisher taken verbatim from the card. Inert untrusted text. */
    @JsonProperty("publisher")
    private String publisher;

    /** Bounded repository provenance declared through the catalog's sourceSet and repoPath metadata. It contains no descriptor URL. Always present, with a path, unless the caller requires agent-plugin-confirmed-installation. Such a caller also receives a repository-root plugin (sourceSet without repoPath) whose source has no path, and a plugin with no usable declaration (neither key, or repoPath alone) and no source; the latter carries a complete materialisation reference and a handle, and the verified descriptor supplies the repository, commit and root when it is planned. Never derived from the catalogue URL. */
    @JsonProperty("source")
    private CatalogPluginRepositorySource source;

    /** Explicit validated compatibility tags, in canonical order. An empty list means the source declared no recognised compatibility; clients must not infer compatibility from other fields. `canvas-only` requires both `canvas` and `github-copilot`. */
    @JsonProperty("compatibilityTags")
    private List<CatalogAgentPluginCompatibilityTag> compatibilityTags;

    /** Whether this plugin can be installed in the selected session. Present only when the caller requires the agent-plugin-confirmed-installation capability. */
    @JsonProperty("installability")
    private CatalogAgentPluginInstallability installability;

    /** Reviewable install plan built only from verified catalogue install data. Present exactly when installability is `installable`; the runtime never derives it from the repository source, display text, or identity. */
    @JsonProperty("installPlan")
    private CatalogAgentPluginInstallPlan installPlan;

    /** Opaque, runtime-instance scoped, TTL-bound, single-use handle for planning this plugin's installation through plugins.planInstall. Present only when the caller requires the agent-plugin-confirmed-installation capability and the candidate carries a complete Agent Finder materialisation reference, which stays runtime-private. Never logged. */
    @JsonProperty("handle")
    private String handle;

    /** ISO 8601 timestamp after which the handle is stale and will be rejected. Present exactly when handle is present. */
    @JsonProperty("handleExpiresAt")
    private String handleExpiresAt;

    /** Where the Agent Plugin catalog reference was observed, without its descriptor, URL, raw data, or content digest. */
    @JsonProperty("provenance")
    private CatalogAgentPluginCandidateProvenance provenance;

    /** How many times the resource has been installed, as reported by the feed. Omitted when the feed reports no count or an invalid one. */
    @JsonProperty("installCount")
    private Long installCount;

    /** HTTPS URL of the resource's icon on raw.githubusercontent.com, as reported by the feed. Untrusted external content: load it without credentials. Omitted when absent or outside the feed's icon host policy. */
    @JsonProperty("iconUrl")
    private String iconUrl;

    /** Identifiers of the connectors the resource works with, as reported by the feed, without repeats. Inert untrusted text. Omitted when the feed reports none. */
    @JsonProperty("connectorIds")
    private List<String> connectorIds;

    /** Category slugs reported by the feed, such as `build` or `review`, without repeats. Values are passed through, so clients must tolerate slugs they do not recognise. Inert untrusted text. Omitted when the feed reports none. */
    @JsonProperty("categories")
    private List<String> categories;

    /** Versioned trust metadata observed from the catalog authority. Optional for protocol-3 compatibility and emitted only when the caller also requires the trust-snapshot capability. */
    @JsonProperty("trust")
    private CatalogTrustSnapshot trust;

    public CatalogAgentPluginMediaType getMediaType() { return mediaType; }
    public void setMediaType(CatalogAgentPluginMediaType mediaType) { this.mediaType = mediaType; }

    public String getIdentity() { return identity; }
    public void setIdentity(String identity) { this.identity = identity; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public CatalogPluginRepositorySource getSource() { return source; }
    public void setSource(CatalogPluginRepositorySource source) { this.source = source; }

    public List<CatalogAgentPluginCompatibilityTag> getCompatibilityTags() { return compatibilityTags; }
    public void setCompatibilityTags(List<CatalogAgentPluginCompatibilityTag> compatibilityTags) { this.compatibilityTags = compatibilityTags; }

    public CatalogAgentPluginInstallability getInstallability() { return installability; }
    public void setInstallability(CatalogAgentPluginInstallability installability) { this.installability = installability; }

    public CatalogAgentPluginInstallPlan getInstallPlan() { return installPlan; }
    public void setInstallPlan(CatalogAgentPluginInstallPlan installPlan) { this.installPlan = installPlan; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getHandleExpiresAt() { return handleExpiresAt; }
    public void setHandleExpiresAt(String handleExpiresAt) { this.handleExpiresAt = handleExpiresAt; }

    public CatalogAgentPluginCandidateProvenance getProvenance() { return provenance; }
    public void setProvenance(CatalogAgentPluginCandidateProvenance provenance) { this.provenance = provenance; }

    public Long getInstallCount() { return installCount; }
    public void setInstallCount(Long installCount) { this.installCount = installCount; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public List<String> getConnectorIds() { return connectorIds; }
    public void setConnectorIds(List<String> connectorIds) { this.connectorIds = connectorIds; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public CatalogTrustSnapshot getTrust() { return trust; }
    public void setTrust(CatalogTrustSnapshot trust) { this.trust = trust; }


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
        private CatalogAgentPluginMediaType mediaType;
        private String identity;
        private String version;
        private String displayName;
        private String description;
        private String publisher;
        private CatalogPluginRepositorySource source;
        private List<CatalogAgentPluginCompatibilityTag> compatibilityTags;
        private CatalogAgentPluginInstallability installability;
        private CatalogAgentPluginInstallPlan installPlan;
        private String handle;
        private String handleExpiresAt;
        private CatalogAgentPluginCandidateProvenance provenance;
        private Long installCount;
        private String iconUrl;
        private List<String> connectorIds;
        private List<String> categories;
        private CatalogTrustSnapshot trust;

        /**
         * Sets the {@code mediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mediaType(CatalogAgentPluginMediaType value) {
            this.mediaType = value;
            return this;
        }

        /**
         * Sets the {@code identity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder identity(String value) {
            this.identity = value;
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
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code publisher} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder publisher(String value) {
            this.publisher = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(CatalogPluginRepositorySource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code compatibilityTags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder compatibilityTags(List<CatalogAgentPluginCompatibilityTag> value) {
            this.compatibilityTags = value;
            return this;
        }

        /**
         * Sets the {@code installability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installability(CatalogAgentPluginInstallability value) {
            this.installability = value;
            return this;
        }

        /**
         * Sets the {@code installPlan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installPlan(CatalogAgentPluginInstallPlan value) {
            this.installPlan = value;
            return this;
        }

        /**
         * Sets the {@code handle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder handle(String value) {
            this.handle = value;
            return this;
        }

        /**
         * Sets the {@code handleExpiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder handleExpiresAt(String value) {
            this.handleExpiresAt = value;
            return this;
        }

        /**
         * Sets the {@code provenance} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provenance(CatalogAgentPluginCandidateProvenance value) {
            this.provenance = value;
            return this;
        }

        /**
         * Sets the {@code installCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installCount(Long value) {
            this.installCount = value;
            return this;
        }

        /**
         * Sets the {@code iconUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder iconUrl(String value) {
            this.iconUrl = value;
            return this;
        }

        /**
         * Sets the {@code connectorIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder connectorIds(List<String> value) {
            this.connectorIds = value;
            return this;
        }

        /**
         * Sets the {@code categories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder categories(List<String> value) {
            this.categories = value;
            return this;
        }

        /**
         * Sets the {@code trust} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder trust(CatalogTrustSnapshot value) {
            this.trust = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CatalogAgentPluginCandidate build() {
            if (mediaType == null) throw new IllegalStateException("Missing required property: mediaType");
            if (identity == null) throw new IllegalStateException("Missing required property: identity");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (compatibilityTags == null) throw new IllegalStateException("Missing required property: compatibilityTags");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            CatalogAgentPluginCandidate instance = new CatalogAgentPluginCandidate();
            instance.setMediaType(mediaType);
            instance.setIdentity(identity);
            instance.setVersion(version);
            instance.setDisplayName(displayName);
            instance.setDescription(description);
            instance.setPublisher(publisher);
            instance.setSource(source);
            instance.setCompatibilityTags(compatibilityTags);
            instance.setInstallability(installability);
            instance.setInstallPlan(installPlan);
            instance.setHandle(handle);
            instance.setHandleExpiresAt(handleExpiresAt);
            instance.setProvenance(provenance);
            instance.setInstallCount(installCount);
            instance.setIconUrl(iconUrl);
            instance.setConnectorIds(connectorIds);
            instance.setCategories(categories);
            instance.setTrust(trust);
            return instance;
        }
    }
}

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
 * An inert MCP server catalog result. Every free-text field is untrusted external data and must never be treated as an instruction, and the handle is the only way to refer to the candidate in a later operation.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogMcpServerCandidate extends CatalogCandidate {

    @JsonProperty("kind")
    private final String kind = "mcp-server";

    @Override
    public String getKind() { return kind; }

    /** Opaque, runtime-instance scoped, TTL-bound, single-use handle for this candidate. Carries no readable information and is rejected when stale, replayed, or presented to a different runtime instance. Never logged. */
    @JsonProperty("handle")
    private String handle;

    /** ISO 8601 timestamp after which the handle is stale and will be rejected. */
    @JsonProperty("handleExpiresAt")
    private String handleExpiresAt;

    /** JSON MCP media type of the underlying card. */
    @JsonProperty("mediaType")
    private McpServerCardMediaType mediaType;

    /** Whether this MCP server can be planned for installation, and if policy prevents it. */
    @JsonProperty("installability")
    private CatalogMcpServerInstallability installability;

    /** Display name taken verbatim from the card. Inert untrusted text. */
    @JsonProperty("displayName")
    private String displayName;

    /** Description taken verbatim from the card. Inert untrusted text. */
    @JsonProperty("description")
    private String description;

    /** Publisher taken verbatim from the card. Inert untrusted text. */
    @JsonProperty("publisher")
    private String publisher;

    /** Where the card came from: exactly one of a URL or embedded data, encoded as a tagged union so neither both nor neither can be represented. */
    @JsonProperty("source")
    private CatalogCandidateSource source;

    /** Where the catalog reference was observed, without the card itself or any content digest. */
    @JsonProperty("provenance")
    private CatalogMcpServerCandidateProvenance provenance;

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

    /** Versioned trust metadata observed from the catalog authority. Optional for protocol-3 compatibility with runtimes that predate trust snapshots. A trust-capable runtime emits an explicit snapshot even when the authority omitted or malformed its trust field. */
    @JsonProperty("trust")
    private CatalogTrustSnapshot trust;

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getHandleExpiresAt() { return handleExpiresAt; }
    public void setHandleExpiresAt(String handleExpiresAt) { this.handleExpiresAt = handleExpiresAt; }

    public McpServerCardMediaType getMediaType() { return mediaType; }
    public void setMediaType(McpServerCardMediaType mediaType) { this.mediaType = mediaType; }

    public CatalogMcpServerInstallability getInstallability() { return installability; }
    public void setInstallability(CatalogMcpServerInstallability installability) { this.installability = installability; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public CatalogCandidateSource getSource() { return source; }
    public void setSource(CatalogCandidateSource source) { this.source = source; }

    public CatalogMcpServerCandidateProvenance getProvenance() { return provenance; }
    public void setProvenance(CatalogMcpServerCandidateProvenance provenance) { this.provenance = provenance; }

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
        private String handle;
        private String handleExpiresAt;
        private McpServerCardMediaType mediaType;
        private CatalogMcpServerInstallability installability;
        private String displayName;
        private String description;
        private String publisher;
        private CatalogCandidateSource source;
        private CatalogMcpServerCandidateProvenance provenance;
        private Long installCount;
        private String iconUrl;
        private List<String> connectorIds;
        private List<String> categories;
        private CatalogTrustSnapshot trust;

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
         * Sets the {@code mediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mediaType(McpServerCardMediaType value) {
            this.mediaType = value;
            return this;
        }

        /**
         * Sets the {@code installability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installability(CatalogMcpServerInstallability value) {
            this.installability = value;
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
        public Builder source(CatalogCandidateSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code provenance} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provenance(CatalogMcpServerCandidateProvenance value) {
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
        public CatalogMcpServerCandidate build() {
            if (handle == null) throw new IllegalStateException("Missing required property: handle");
            if (handleExpiresAt == null) throw new IllegalStateException("Missing required property: handleExpiresAt");
            if (mediaType == null) throw new IllegalStateException("Missing required property: mediaType");
            if (installability == null) throw new IllegalStateException("Missing required property: installability");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            CatalogMcpServerCandidate instance = new CatalogMcpServerCandidate();
            instance.setHandle(handle);
            instance.setHandleExpiresAt(handleExpiresAt);
            instance.setMediaType(mediaType);
            instance.setInstallability(installability);
            instance.setDisplayName(displayName);
            instance.setDescription(description);
            instance.setPublisher(publisher);
            instance.setSource(source);
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

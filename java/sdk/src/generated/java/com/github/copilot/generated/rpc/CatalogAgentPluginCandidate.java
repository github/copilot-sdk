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
}

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
 * An inert Agent Plugin catalog result. Its canonical catalog identity, declared version, repository source claim, and explicit compatibility tags are safe to correlate, while its descriptor, URL, raw data, and installed-plugin state remain runtime-private. This contract-only variant does not mint or expose a candidate handle.
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

    /** Canonical Agent Plugin media type. */
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

    /** Bounded repository provenance declared through the catalog's sourceSet and repoPath metadata. It contains no descriptor URL. */
    @JsonProperty("source")
    private CatalogPluginRepositorySource source;

    /** Explicit validated compatibility tags, in canonical order. An empty list means the source declared no recognised compatibility; clients must not infer compatibility from other fields. `canvas-only` requires both `canvas` and `github-copilot`. */
    @JsonProperty("compatibilityTags")
    private List<CatalogAgentPluginCompatibilityTag> compatibilityTags;

    /** Where the Agent Plugin catalog reference was observed, without its descriptor, URL, raw data, or content digest. */
    @JsonProperty("provenance")
    private CatalogAgentPluginCandidateProvenance provenance;

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

    public CatalogAgentPluginCandidateProvenance getProvenance() { return provenance; }
    public void setProvenance(CatalogAgentPluginCandidateProvenance provenance) { this.provenance = provenance; }

    public CatalogTrustSnapshot getTrust() { return trust; }
    public void setTrust(CatalogTrustSnapshot trust) { this.trust = trust; }
}

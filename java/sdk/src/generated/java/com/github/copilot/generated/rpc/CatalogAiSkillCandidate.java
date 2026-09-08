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
 * An inert AI skill catalog result. AI skills are discovery-only and cannot be represented as installable through this surface.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogAiSkillCandidate extends CatalogCandidate {

    @JsonProperty("kind")
    private final String kind = "ai-skill";

    @Override
    public String getKind() { return kind; }

    /** Opaque, runtime-instance scoped, TTL-bound, single-use handle for this candidate. Carries no readable information and is rejected when stale, replayed, or presented to a different runtime instance. Never logged. */
    @JsonProperty("handle")
    private String handle;

    /** ISO 8601 timestamp after which the handle is stale and will be rejected. */
    @JsonProperty("handleExpiresAt")
    private String handleExpiresAt;

    /** Media type of the underlying AI skill card */
    @JsonProperty("mediaType")
    private String mediaType;

    /** AI skills are discovery-only and cannot be installed through this surface */
    @JsonProperty("installability")
    private String installability;

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
    private CatalogAiSkillCandidateProvenance provenance;

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getHandleExpiresAt() { return handleExpiresAt; }
    public void setHandleExpiresAt(String handleExpiresAt) { this.handleExpiresAt = handleExpiresAt; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getInstallability() { return installability; }
    public void setInstallability(String installability) { this.installability = installability; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public CatalogCandidateSource getSource() { return source; }
    public void setSource(CatalogCandidateSource source) { this.source = source; }

    public CatalogAiSkillCandidateProvenance getProvenance() { return provenance; }
    public void setProvenance(CatalogAiSkillCandidateProvenance provenance) { this.provenance = provenance; }
}

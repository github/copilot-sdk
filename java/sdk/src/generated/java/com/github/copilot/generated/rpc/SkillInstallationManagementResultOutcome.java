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
 * Variant {@code outcome} of {@link SkillInstallationManagementResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillInstallationManagementResultOutcome extends SkillInstallationManagementResult {

    @JsonProperty("kind")
    private final String kind = "outcome";

    @Override
    public String getKind() { return kind; }

    /** Observed management outcome. */
    @JsonProperty("outcome")
    private SkillInstallationManagementOutcome outcome;

    /** Capabilities honoured for this request. */
    @JsonProperty("negotiated")
    private CatalogNegotiatedContract negotiated;

    public SkillInstallationManagementOutcome getOutcome() { return outcome; }
    public void setOutcome(SkillInstallationManagementOutcome outcome) { this.outcome = outcome; }

    public CatalogNegotiatedContract getNegotiated() { return negotiated; }
    public void setNegotiated(CatalogNegotiatedContract negotiated) { this.negotiated = negotiated; }
}

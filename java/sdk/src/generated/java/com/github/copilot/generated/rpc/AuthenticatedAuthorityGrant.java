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
 * Bounded, lifecycle-fenced host authority rendered to the Assisted Permissions judge. Source-specific meaning and scope remain opaque data interpreted by the judge. The source must match the enclosing update source. At most 8 statements of up to 1,024 characters each (4,096 total); scope follows the assisted-approval context JSON bounds. A session holds at most 8 active sources and 32 KiB of rendered grants.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticatedAuthorityGrant(
    /** Stable identifier for the trusted authority source; must match the update source. */
    @JsonProperty("source") String source,
    /** Stable host-issued identifier for this authority. */
    @JsonProperty("grantId") String grantId,
    /** RFC 3339 expiry timestamp. */
    @JsonProperty("expiresAt") String expiresAt,
    /** Bounded human-readable authority statements rendered to the judge. */
    @JsonProperty("statements") List<String> statements,
    /** Structured scope facts constraining the authority statements. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("scope") Object scope
) {
}

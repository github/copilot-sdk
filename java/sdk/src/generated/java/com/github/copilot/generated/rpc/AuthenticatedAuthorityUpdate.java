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
 * Replaces or revokes one source's revocable session-scoped authority from the owning host. A non-null grant replaces only the named source; null revokes only the named source. The revision always advances the session-global fence.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticatedAuthorityUpdate(
    /** Monotonically increasing host-issued revision, from 1 to 2^53 - 1 so every SDK represents it exactly. */
    @JsonProperty("revision") Long revision,
    /** Stable source whose independent authority slot is being updated. */
    @JsonProperty("source") String source,
    /** Replacement authority, or null to revoke the named source. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("grant") AuthenticatedAuthorityGrant grant
) {
}

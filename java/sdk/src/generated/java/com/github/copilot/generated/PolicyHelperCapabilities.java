/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Policy Helper failure and recovery features implemented by this runtime. Capability presence, not absence of an optional failure, determines whether a client may rely on these contracts.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyHelperCapabilities(
    /** The runtime emits PolicyHelperFailure on shared managed-settings snapshots and applicable structured operation errors. */
    @JsonProperty("structuredFailure") Boolean structuredFailure,
    /** The runtime supports session.managedSettings.recheckPolicyHelper for a fresh scoped helper evaluation. */
    @JsonProperty("recheck") Boolean recheck
) {
}

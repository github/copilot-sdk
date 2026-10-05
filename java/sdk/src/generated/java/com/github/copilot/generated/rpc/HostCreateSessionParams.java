/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * One application-owned session handoff, requested by the supervised hosting participant.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostCreateSessionParams(
    /** Unique identity for this participation, independent of the session lifetime. */
    @JsonProperty("handoffId") String handoffId,
    /** Resume an app-owned durable session instead of creating a new session. */
    @JsonProperty("resume") Boolean resume,
    /** Host-selected SDK creation or resume settings, without executable callbacks or tools. */
    @JsonProperty("config") Map<String, Object> config
) {
}

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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Bounded batch request for client-owned metadata from persisted local sessions.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsGetClientMetadataParams(
    /** Session IDs to inspect. Results preserve this order. */
    @JsonProperty("sessionIds") List<String> sessionIds,
    /** Case-sensitive keys to project from each valid bag. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces. Omit to return every entry. */
    @JsonProperty("keys") List<String> keys
) {
}

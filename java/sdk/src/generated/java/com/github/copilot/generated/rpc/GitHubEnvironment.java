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
 * Safe discovery information. Host-side relay bootstrap credentials are never included.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubEnvironment(
    /** Identifier assigned by Mission Control. */
    @JsonProperty("id") String id,
    /** Human-readable environment name. */
    @JsonProperty("name") String name,
    /** Compute kind reported by Mission Control. */
    @JsonProperty("kind") EnvironmentKind kind,
    /** Open-ended operational status vocabulary. */
    @JsonProperty("status") String status,
    /** Hosting capabilities advertised by the environment. */
    @JsonProperty("capabilities") EnvironmentCapabilities capabilities,
    /** Identifier of the environment owner. */
    @JsonProperty("ownerId") String ownerId,
    /** Owner category reported by Mission Control. */
    @JsonProperty("ownerType") String ownerType,
    /** Organization identifier, when the environment belongs to an organization. */
    @JsonProperty("orgId") String orgId,
    /** Discovery labels attached to the environment. */
    @JsonProperty("labels") Map<String, String> labels,
    /** Timestamp of the last heartbeat received by Mission Control. */
    @JsonProperty("lastHeartbeatAt") String lastHeartbeatAt
) {
}

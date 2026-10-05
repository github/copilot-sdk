/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Application callback routed over its existing SDK connection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostSessionCreateCallback(
    /** Listener UUID identifying the owning application's host. */
    @JsonProperty("hostId") String hostId,
    /** Unique identity of the session participation being requested. */
    @JsonProperty("handoffId") String handoffId,
    /** Resume an app-owned durable session instead of creating a new session. */
    @JsonProperty("resume") Boolean resume,
    /** Host-selected SDK creation or resume settings, without executable callbacks or tools. */
    @JsonProperty("config") Map<String, Object> config
) {
}

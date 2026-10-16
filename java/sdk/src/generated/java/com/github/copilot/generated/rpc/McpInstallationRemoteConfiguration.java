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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Final remote configuration, not a template. The producer refuses external-value
expansion before presenting this review. Receipt-owned secrets appear only as
`${installation-secret:<id>}` references whose `<id>` matches a reviewed
`${secret:<id>}` placeholder; values are never included.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallationRemoteConfiguration(
    /** Transport in the effective persisted remote configuration. */
    @JsonProperty("transport") McpPlanRemoteTransport transport,
    /** Exact resolved endpoint, without templates or secret placeholders. */
    @JsonProperty("url") String url,
    /** Configured headers, excluding separately authorised OAuth tokens. Values may
contain owned secret references, never secret values. */
    @JsonProperty("headers") Map<String, String> headers,
    /** Configured tool selection, not permission to invoke those tools. */
    @JsonProperty("tools") List<String> tools
) {
}

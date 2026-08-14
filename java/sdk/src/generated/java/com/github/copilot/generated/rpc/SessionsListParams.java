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
import javax.annotation.processing.Generated;

/**
 * Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsListParams(
    /** Which session sources to include. Defaults to `local` for backward compatibility. */
    @JsonProperty("source") SessionSource source,
    /** When provided, only the first N local sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every local session. Has no effect on remote entries (which always carry their full shape). */
    @JsonProperty("metadataLimit") Long metadataLimit,
    /** Optional filter applied to the returned sessions */
    @JsonProperty("filter") SessionListFilter filter,
    /** When true, include detached maintenance sessions. Defaults to false for user-facing session lists. */
    @JsonProperty("includeDetached") Boolean includeDetached,
    /** Only meaningful when `source` includes remote. When true, propagates errors from the remote service instead of silently returning an empty remote list. Defaults to false. */
    @JsonProperty("throwOnError") Boolean throwOnError
) {
}

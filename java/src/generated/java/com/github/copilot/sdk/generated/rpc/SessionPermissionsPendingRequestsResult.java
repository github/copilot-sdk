/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * List of pending permission requests reconstructed from event history.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsPendingRequestsResult(
    /** Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session. */
    @JsonProperty("items") List<PendingPermissionRequest> items
) {
}

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
 * Outcome of a session mode change, including any model switch it triggered and follow-up the host must perform.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModeSetResult(
    /** Lifecycle status of the requested mode change. */
    @JsonProperty("status") String status,
    /** Whether applying the mode changed the active model. */
    @JsonProperty("modelChanged") Boolean modelChanged,
    /** Compaction confirmation required before the mode change can complete. */
    @JsonProperty("confirmation") ModelSwitchConfirmation confirmation,
    /** User-facing warning produced while applying the mode change. */
    @JsonProperty("warning") String warning,
    /** User-facing outcome message for the model switch triggered by the mode change. */
    @JsonProperty("message") String message,
    /** Deprecation warnings associated with the model selected by the mode change. */
    @JsonProperty("deprecationWarnings") List<String> deprecationWarnings,
    /** Whether the host must defer implementing the requested mode change. */
    @JsonProperty("deferImplementation") Boolean deferImplementation,
    /** Whether the host should arm an interactive continuation after the mode change. */
    @JsonProperty("armInteractiveContinuation") Boolean armInteractiveContinuation
) {
}

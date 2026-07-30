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
 * Complete current or terminal factory run envelope.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFactoryRunResult(
    /** Factory run identifier. */
    @JsonProperty("runId") String runId,
    /** Current or terminal factory run status. */
    @JsonProperty("status") FactoryRunStatus status,
    /** Completed factory result. */
    @JsonProperty("result") Object result,
    /** Error message for an errored run. */
    @JsonProperty("error") String error,
    /** Machine-readable failure details for an errored run. */
    @JsonProperty("failure") Object failure,
    /** Reason for a halted or cancelled run. */
    @JsonProperty("reason") String reason,
    /** Partial journal and progress snapshot for a halted, cancelled, or errored run. */
    @JsonProperty("snapshot") Object snapshot
) {
}

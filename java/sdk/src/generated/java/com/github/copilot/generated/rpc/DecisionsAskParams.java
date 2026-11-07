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
 * One request to a decision model: shared context plus the questions to answer about it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionsAskParams(
    /** Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used. */
    @JsonProperty("selectionId") String selectionId,
    /** GitHub token to ask as. When provided, it is used instead of the current account or `selectionId`. */
    @JsonProperty("gitHubToken") String gitHubToken,
    /** Decision model id from `models.list` `decisionModels`. When omitted, the runtime uses its default decision model for the account. */
    @JsonProperty("model") String model,
    /** Shared context every question sees. The runtime never truncates it; an oversized request fails with `tooLarge`. */
    @JsonProperty("context") Object context,
    /** Questions to answer about the context. Names must be unique; answers come back in the same order. */
    @JsonProperty("questions") List<Object> questions,
    /** End-to-end deadline in milliseconds, covering credential resolution, queueing, the model call and parsing. Defaults to 5000; allowed range 250-30000. */
    @JsonProperty("timeoutMs") Long timeoutMs,
    /** Static identifier of the calling feature, used only for telemetry and never sent to the model, for example `vscode.chat.route`. Unrecognized values are reported as `other`. */
    @JsonProperty("purpose") String purpose
) {
}

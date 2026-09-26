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
 * Indicates whether the message was delivered, with an error message when delivery failed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionTasksSendMessageResult(
    /** Optional exact queue admission receipt on sent=true only. No implicit event or execution-success claim. */
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.github.copilot.WorkerCausalityDeserializer.class)
    @JsonProperty("workerCausality") WorkerCausality workerCausality,
    /** Whether the message was successfully delivered or steered */
    @JsonProperty("sent") Boolean sent,
    /** Error message if delivery failed */
    @JsonProperty("error") String error
) {
    /** Creates a value without optional worker diagnostics. */
    public SessionTasksSendMessageResult(Boolean sent, String error) {
        this(null, sent, error);
    }
}

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
 * A memory the agent stored after validation, secret filtering and approval.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record MemoryStoreStoreParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Short topic the memory is about. */
    @JsonProperty("subject") String subject,
    /** The fact to remember. */
    @JsonProperty("fact") String fact,
    /** Sources supporting the fact, such as file paths with line numbers or a quotation of the user. */
    @JsonProperty("citations") String citations,
    /** Why the agent considers the fact worth remembering. */
    @JsonProperty("reason") String reason
) {
}

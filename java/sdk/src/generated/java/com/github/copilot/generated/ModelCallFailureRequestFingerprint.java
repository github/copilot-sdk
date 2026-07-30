/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Content-free structural summary of the failing request for diagnosing malformed 4xx calls
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCallFailureRequestFingerprint(
    /** Total number of messages in the request */
    @JsonProperty("messageCount") Long messageCount,
    /** Number of "tool" result messages in the request */
    @JsonProperty("toolResultMessageCount") Long toolResultMessageCount,
    /** Total number of tool calls across assistant messages */
    @JsonProperty("toolCallCount") Long toolCallCount,
    /** Tool calls whose name is missing or empty (rejected by strict providers) */
    @JsonProperty("namelessToolCallCount") Long namelessToolCallCount,
    /** Total number of image content parts */
    @JsonProperty("imagePartCount") Long imagePartCount,
    /** Image parts whose media type cannot be determined (rejected by strict providers) */
    @JsonProperty("imagePartsMissingMediaType") Long imagePartsMissingMediaType,
    /** Role of the final message in the request */
    @JsonProperty("lastMessageRole") String lastMessageRole
) {
}

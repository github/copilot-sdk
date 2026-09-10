/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandTimelineEntry(
    /** Timeline entry presentation type. */
    @JsonProperty("type") String type,
    /** Text displayed for the timeline entry. */
    @JsonProperty("text") String text,
    /** Optional URL associated with the timeline entry. */
    @JsonProperty("url") String url,
    /** What the user must do to recover, when the entry reports a failure the runtime knows an action for. The `text` never names a client affordance, so a client that offers one renders it from this value. */
    @JsonProperty("remediation") RemediationAction remediation
) {
}

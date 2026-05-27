/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Schema for the `ToolExecutionCompleteUIResourceMetaUICsp` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUICsp(
    @JsonProperty("connectDomains") List<String> connectDomains,
    @JsonProperty("resourceDomains") List<String> resourceDomains,
    @JsonProperty("frameDomains") List<String> frameDomains,
    @JsonProperty("baseUriDomains") List<String> baseUriDomains
) {
}

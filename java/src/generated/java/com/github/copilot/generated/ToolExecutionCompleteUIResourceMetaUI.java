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
 * Schema for the `ToolExecutionCompleteUIResourceMetaUI` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResourceMetaUI(
    /** Schema for the `ToolExecutionCompleteUIResourceMetaUICsp` type. */
    @JsonProperty("csp") ToolExecutionCompleteUIResourceMetaUICsp csp,
    /** Schema for the `ToolExecutionCompleteUIResourceMetaUIPermissions` type. */
    @JsonProperty("permissions") ToolExecutionCompleteUIResourceMetaUIPermissions permissions,
    @JsonProperty("domain") String domain,
    @JsonProperty("prefersBorder") Boolean prefersBorder
) {
}

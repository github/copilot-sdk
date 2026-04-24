/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Extension(
    /** Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper') */
    @JsonProperty("id") String id,
    /** Extension name (directory name) */
    @JsonProperty("name") String name,
    /** Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/) */
    @JsonProperty("source") ExtensionSource source,
    /** Current status: running, disabled, failed, or starting */
    @JsonProperty("status") ExtensionStatus status,
    /** Process ID if the extension is running */
    @JsonProperty("pid") Long pid
) {
}

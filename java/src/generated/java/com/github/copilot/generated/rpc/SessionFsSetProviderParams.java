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

/**
 * Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSetProviderParams(
    /** Initial working directory for sessions */
    @JsonProperty("initialCwd") String initialCwd,
    /** Path within each session's SessionFs where the runtime stores files for that session */
    @JsonProperty("sessionStatePath") String sessionStatePath,
    /** Path conventions used by this filesystem */
    @JsonProperty("conventions") SessionFsSetProviderConventions conventions,
    /** Optional capabilities declared by the provider */
    @JsonProperty("capabilities") SessionFsSetProviderCapabilities capabilities
) {
}

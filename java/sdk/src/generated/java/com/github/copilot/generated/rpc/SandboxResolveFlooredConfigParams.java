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
 * The user's sandbox settings and the managed sandbox floor to raise them to.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxResolveFlooredConfigParams(
    /** The user's own `sandbox` settings slice. Omitted when the user has configured none, which still yields the managed floor rather than a disabled sandbox. */
    @JsonProperty("userSandbox") Object userSandbox,
    /** The effective managed `sandbox` slice acting as a floor. Omitted when no administrator policy applies, in which case the user's own settings stand unchanged. */
    @JsonProperty("managedSandbox") Object managedSandbox
) {
}

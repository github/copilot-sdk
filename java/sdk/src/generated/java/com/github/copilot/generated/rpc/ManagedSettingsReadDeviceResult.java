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
 * The device-managed settings layer, with the sandbox floor and the load outcome a host needs to compose it into an effective policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsReadDeviceResult(
    /** Validated, canonical device-managed settings. Omitted when no device policy was discovered, when the discovered document failed validation, or when it validated to an empty object — an empty document carries no policy, and reporting it as present would outrank layers that do. */
    @JsonProperty("settings") Object settings,
    /** Sandbox floor read from the discovered document before validation, so a document that is otherwise rejected can still raise the floor it declared. */
    @JsonProperty("sandboxFloor") Object sandboxFloor,
    /** Whether discovery or validation failed. A host must treat this as "policy unknown" rather than "no policy": absent settings with this set false mean the device genuinely has none. */
    @JsonProperty("loadFailed") Boolean loadFailed
) {
}

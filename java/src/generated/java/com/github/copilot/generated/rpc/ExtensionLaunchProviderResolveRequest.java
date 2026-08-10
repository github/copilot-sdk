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
 * A discovered extension entrypoint that the registered integrator may classify and resolve to an opaque launch profile.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionLaunchProviderResolveRequest(
    /** Source-qualified extension identifier. */
    @JsonProperty("id") String id,
    /** Human-readable extension name. */
    @JsonProperty("name") String name,
    /** Absolute path to the discovered extension entrypoint. */
    @JsonProperty("modulePath") String modulePath,
    /** Discovery source for the extension entrypoint. */
    @JsonProperty("source") ExtensionSource source
) {
}

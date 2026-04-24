/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtensionsLoadedExtension(
    /** Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper') */
    @JsonProperty("id") String id,
    /** Extension name (directory name) */
    @JsonProperty("name") String name,
    /** Discovery source */
    @JsonProperty("source") ExtensionsLoadedExtensionSource source,
    /** Current status: running, disabled, failed, or starting */
    @JsonProperty("status") ExtensionsLoadedExtensionStatus status
) {
}

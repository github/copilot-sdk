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
 * One bounded field-specific finding reported by a Policy Helper. The path is a helper-defined logical configuration path, not necessarily a filesystem path.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyHelperFailureDetail(
    /** Optional helper-defined logical configuration path. */
    @JsonProperty("path") String path,
    /** Bounded helper-authored plain-text explanation for this finding. */
    @JsonProperty("message") String message
) {
}

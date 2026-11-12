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
 * The unrecognized top-level keys found in the user settings document, already phrased for display.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsUnknownKeysWarningResult(
    /** The warning to show the user, naming the unrecognized keys and the file they were found in. Absent when every top-level key is recognized, and also when the document is missing or could not be read, because an unknown-key list taken from a document the runtime could not load would name keys that may not be there. */
    @JsonProperty("warning") String warning
) {
}

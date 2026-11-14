/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The user's stored settings, together with any diagnostic raised while reading them.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsLoadWithWarningResult(
    /** The stored user settings document, keyed by setting name, with each value carried verbatim. Empty when the file could not be read at all. */
    @JsonProperty("settings") Map<String, Object> settings,
    /** Why part of the document was ignored, phrased for display to the user. Null when the document loaded cleanly. The settings that did load are still reported, so a consumer may surface this and continue rather than treat it as a failure. */
    @JsonProperty("warning") String warning
) {
}

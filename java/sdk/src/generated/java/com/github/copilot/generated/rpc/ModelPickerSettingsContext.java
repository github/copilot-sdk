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
 * Filesystem and environment context used to resolve model-picker settings.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelPickerSettingsContext(
    /** Optional Copilot configuration directory containing persisted settings. */
    @JsonProperty("configDir") String configDir,
    /** User home directory used when resolving persisted settings. */
    @JsonProperty("homeDirectory") String homeDirectory,
    /** Environment variables consulted while resolving model-picker settings. */
    @JsonProperty("environment") Map<String, Object> environment
) {
}

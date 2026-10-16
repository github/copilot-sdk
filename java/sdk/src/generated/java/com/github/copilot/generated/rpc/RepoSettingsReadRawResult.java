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
 * Parsed repository settings without normalization or layering.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RepoSettingsReadRawResult(
    /** Stored JSON values after the existing JSONC parser and prototype-safety checks. Empty when the file is missing, blank, or invalid. */
    @JsonProperty("config") Object config,
    /** True when the file exists but did not parse, so `config` is empty for that reason rather than because nothing is set. */
    @JsonProperty("invalid") Boolean invalid
) {
}

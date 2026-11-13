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
 * A request-local value for one exact reviewed placeholder. Never logged or persisted in a plan.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpInstallationSecret(
    /** Exact placeholder from the selected choice, not a caller-chosen backend identifier. */
    @JsonProperty("placeholder") String placeholder,
    /** Fresh explicit secret value. It is omitted from confirmation reviews and telemetry. */
    @JsonProperty("value") String value
) {
}

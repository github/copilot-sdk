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
 * A service-published message about a model, carrying a stable machine-readable code alongside human-readable text.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelMessage(
    /** Stable machine-readable identifier for the message, such as `client_version_deprecated`. Hosts can key custom presentation off this; unrecognized codes should fall back to displaying `message`. */
    @JsonProperty("code") String code,
    /** Human-readable message text intended for display to the user. */
    @JsonProperty("message") String message
) {
}

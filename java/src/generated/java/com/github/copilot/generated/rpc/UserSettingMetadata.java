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
 * A single user setting's effective value alongside its default, so consumers can render settings left at their default.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSettingMetadata(
    /** The effective value: the user's value if set, otherwise the default. */
    @JsonProperty("value") Object value,
    /** The centrally-known default for this setting (null when no default is registered). */
    @JsonProperty("default") Object default_,
    /** True when the user has not set an explicit value for this setting (i.e. it is left at its default). Reflects whether the user has overridden the key, not whether the effective value happens to equal the default — a key explicitly set to a value identical to the default still reports false. */
    @JsonProperty("isDefault") Boolean isDefault
) {
}

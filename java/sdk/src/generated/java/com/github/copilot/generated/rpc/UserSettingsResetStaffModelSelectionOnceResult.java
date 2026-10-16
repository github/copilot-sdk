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
 * The outcome of the one-time staff model-selection reset.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsResetStaffModelSelectionOnceResult(
    /** Whether the persisted model selection was cleared. Only `true` means the settings document changed and the host should adopt the returned settings. */
    @JsonProperty("didReset") Boolean didReset,
    /** The timestamp the host should now record, as an ISO-8601 timestamp. Carries the caller's own value back unchanged when nothing was reset. */
    @JsonProperty("resetAt") String resetAt,
    /** The cutoff this reset is gated on, as an ISO-8601 timestamp. A recorded timestamp at or after it means the reset is already done. */
    @JsonProperty("cutoff") String cutoff,
    /** The settings document as it stands after the reset, keyed by setting name. Present only when `didReset` is true. */
    @JsonProperty("settings") Map<String, Object> settings
) {
}

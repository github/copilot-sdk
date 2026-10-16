/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Lock state and provenance of one managed setting.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingMeta(
    /** Whether users and repositories may choose a different value. `false` means policy locks the value. */
    @JsonProperty("overridable") Boolean overridable,
    /** Channel that supplied this scalar value, matching a `layers[].source`: `device`, `server`, or `policyHelper`. These scalar defaults select one winning channel, not a mixed source. Treat unknown values as additional channels; more may be added. */
    @JsonProperty("source") String source,
    /** Original managed value when the runtime adjusted it to a supported effective value. Omitted when no adjustment was needed. */
    @JsonProperty("requested") String requested
) {
}

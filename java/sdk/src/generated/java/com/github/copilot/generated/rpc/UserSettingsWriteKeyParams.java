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
 * A raw top-level setting edit with explicit read-failure behavior.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsWriteKeyParams(
    /** The top-level setting to write without canonicalizing legacy siblings. */
    @JsonProperty("key") String key,
    /** Optional host paths and environment. Omit to use the server's settings home. */
    @JsonProperty("context") UserSettingsHostContext context,
    /** The value to store. Explicit null is retained. Pass null when deleting. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value,
    /** Remove the key instead of writing it. Defaults to false. */
    @JsonProperty("delete") Boolean delete,
    /** Refuse to replace an unreadable or invalid file. Defaults to false. */
    @JsonProperty("refuseOnReadFailure") Boolean refuseOnReadFailure
) {
}

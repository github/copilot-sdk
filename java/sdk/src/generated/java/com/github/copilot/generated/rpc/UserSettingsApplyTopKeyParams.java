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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A single top-level setting subtree to validate and write.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsApplyTopKeyParams(
    /** Canonical path of the setting the caller is editing. Currently informational only. */
    @JsonProperty("canonicalPath") List<String> canonicalPath,
    /** Top-level setting name to write. */
    @JsonProperty("topKey") String topKey,
    /** Value to write for the whole top-level key. Omitted when `delete` is true. Explicit null is preserved and validated as null. */
    @JsonProperty("value") Object value,
    /** True to remove the top-level key instead of writing it. */
    @JsonProperty("delete") Boolean delete,
    /** Records that the user chose an update channel explicitly, so the one-time staff channel migration stops reapplying. Honored only for the `autoUpdatesChannel` key. */
    @JsonProperty("completeStaffUpdateChannelMigration") Boolean completeStaffUpdateChannelMigration
) {
}

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
 * A folder to check against a persisted trusted-folders list, and the configuration directory holding that list.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionsFolderTrustCheckTrustParams(
    /** Folder to check. A folder counts as trusted when it is itself listed, when an ancestor is listed, or when its git root is listed. */
    @JsonProperty("path") String path,
    /** Configuration directory to read the trusted-folders list from. Omit it to read the default location; supply it when the host has redirected its configuration, or the check answers from a list the host is not using. */
    @JsonProperty("configDir") String configDir
) {
}

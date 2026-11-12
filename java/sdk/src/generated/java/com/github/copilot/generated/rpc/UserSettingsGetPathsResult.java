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
 * On-disk locations of the settings files this runtime reads. A path is reported whether or not the file exists.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsGetPathsResult(
    /** The user settings file: `settings.json` under the resolved Copilot home. This is the file `user.settings.get`, `user.settings.load`, and `user.settings.set` read and write. */
    @JsonProperty("userSettings") String userSettings,
    /** The repository's checked-in settings file, or null when no repository root was supplied. */
    @JsonProperty("repoSettings") String repoSettings,
    /** The repository's local, untracked settings file, or null when no repository root was supplied. */
    @JsonProperty("repoLocalSettings") String repoLocalSettings
) {
}

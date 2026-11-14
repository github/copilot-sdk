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
 * The outcome of the one-time obsolete-settings cleanup.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMigrateObsoleteOnDiskResult(
    /** What the cleanup did. Only `changed` rewrote the document. The cleaned settings are not returned here: the document is written before this replies, so `user.settings.load` reads it authoritatively. */
    @JsonProperty("status") UserSettingsMigrateObsoleteOnDiskStatus status
) {
}

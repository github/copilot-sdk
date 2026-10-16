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
 * The outcome of the one-time staff update-channel migration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMigrateStaffUpdateChannelOnceResult(
    /** What the migration did. Only `migrated` means the persisted settings changed. */
    @JsonProperty("status") StaffUpdateChannelMigrationStatus status,
    /** When the migration was recorded as done, as an ISO-8601 timestamp. Absent only for `ineligible`, which records nothing. */
    @JsonProperty("completedAt") String completedAt
) {
}

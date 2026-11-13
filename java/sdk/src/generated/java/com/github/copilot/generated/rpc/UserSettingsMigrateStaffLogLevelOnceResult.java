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
 * The outcome of the one-time staff log-level migration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMigrateStaffLogLevelOnceResult(
    /** What the migration did. Only `refreshed` can have changed the persisted settings. */
    @JsonProperty("status") StaffLogLevelMigrationStatus status,
    /** When the migration was recorded as done, as an ISO-8601 timestamp. Absent only for `ineligible`, which records nothing. */
    @JsonProperty("completedAt") String completedAt,
    /** The settings document as it stands after the migration, keyed by setting name. Present only for `refreshed`, and only when the document could be re-read cleanly. */
    @JsonProperty("settings") Map<String, Object> settings
) {
}

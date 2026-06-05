/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Summary of persisted location permissions applied to the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsLocationsApplyResult(
    /** Location key used in the location-permissions store */
    @JsonProperty("locationKey") String locationKey,
    /** Whether the location is a git repo or directory */
    @JsonProperty("locationType") PermissionLocationType locationType,
    /** Whether a different location was applied since the previous apply call */
    @JsonProperty("changed") Boolean changed,
    /** Number of location-scoped rules added to the live permission service */
    @JsonProperty("appliedRuleCount") Long appliedRuleCount,
    /** Number of persisted allowed directories added to the live path manager */
    @JsonProperty("appliedDirectoryCount") Long appliedDirectoryCount,
    /** Location-scoped rules applied to the live permission service */
    @JsonProperty("appliedRules") List<PermissionRule> appliedRules
) {
}

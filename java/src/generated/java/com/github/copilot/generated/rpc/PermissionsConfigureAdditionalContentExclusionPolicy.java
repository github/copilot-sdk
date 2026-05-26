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
 * Schema for the `PermissionsConfigureAdditionalContentExclusionPolicy` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionsConfigureAdditionalContentExclusionPolicy(
    @JsonProperty("rules") List<PermissionsConfigureAdditionalContentExclusionPolicyRule> rules,
    @JsonProperty("last_updated_at") Object lastUpdatedAt,
    /** Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration. */
    @JsonProperty("scope") PermissionsConfigureAdditionalContentExclusionPolicyScope scope
) {
}

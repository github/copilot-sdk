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
 * Patch of permission policy fields to apply (omit a field to leave it unchanged).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsConfigureParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged. */
    @JsonProperty("approveAllToolPermissionRequests") Boolean approveAllToolPermissionRequests,
    /** If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged. */
    @JsonProperty("approveAllReadPermissionRequests") Boolean approveAllReadPermissionRequests,
    /** If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged. */
    @JsonProperty("rules") PermissionRulesSet rules,
    /** If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged. */
    @JsonProperty("paths") PermissionPathsConfig paths,
    /** If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged. */
    @JsonProperty("urls") PermissionUrlsConfig urls,
    /** If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged. */
    @JsonProperty("additionalContentExclusionPolicies") List<PermissionsConfigureAdditionalContentExclusionPolicy> additionalContentExclusionPolicies
) {
}

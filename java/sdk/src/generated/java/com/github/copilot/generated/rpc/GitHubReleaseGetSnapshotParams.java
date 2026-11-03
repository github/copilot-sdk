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
 * Immutable options for one connection-owned release snapshot.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseGetSnapshotParams(
    /** An unused id from gitHubRelease.allocateRequestId. Reuse the same id and options to read the same snapshot. */
    @JsonProperty("requestId") Long requestId,
    /** The release channel used for update selection. */
    @JsonProperty("updateChannel") GitHubReleaseUpdateChannel updateChannel,
    /** The version embedded in the caller's build. */
    @JsonProperty("buildVersion") String buildVersion,
    /** Optional runtime version override. Invalid or noncanonical values do not replace buildVersion. */
    @JsonProperty("cliVersionOverride") String cliVersionOverride,
    /** The embedded package identity. Omission uses the existing development-version fallback rule. */
    @JsonProperty("packageIdentity") GitHubReleasePackageIdentity packageIdentity
) {
}

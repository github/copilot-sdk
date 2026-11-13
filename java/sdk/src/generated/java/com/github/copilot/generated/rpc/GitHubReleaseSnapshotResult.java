/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The shared release result with its original lookup duration. Runtime worker faults remain RPC errors.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "outcome", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GitHubReleaseSnapshotResultSuccess.class, name = "success"),
    @JsonSubTypes.Type(value = GitHubReleaseSnapshotResultFailed.class, name = "failed")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
abstract class GitHubReleaseSnapshotResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the outcome discriminator
     */
    public abstract String getOutcome();
}

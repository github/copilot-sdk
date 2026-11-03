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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Release facts needed for update selection and admission. GitHub field names remain unchanged.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseSnapshotEntry(
    /** The release tag. */
    @JsonProperty("tag_name") String tagName,
    /** Whether GitHub marks this release as a prerelease. */
    @JsonProperty("prerelease") Boolean prerelease,
    /** Whether GitHub marks this release as a draft. */
    @JsonProperty("draft") Boolean draft,
    /** The published release assets. */
    @JsonProperty("assets") List<GitHubReleaseSnapshotAsset> assets
) {
}

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
 * Redacted repository and GitHub host settings for a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSettingsRepoSnapshot(
    @JsonProperty("name") String name,
    @JsonProperty("id") Double id,
    @JsonProperty("branch") String branch,
    @JsonProperty("commit") String commit,
    @JsonProperty("readWrite") Boolean readWrite,
    @JsonProperty("ownerName") String ownerName,
    @JsonProperty("ownerId") Double ownerId,
    @JsonProperty("serverUrl") String serverUrl,
    @JsonProperty("host") String host,
    @JsonProperty("hostProtocol") String hostProtocol,
    @JsonProperty("secretScanningUrl") String secretScanningUrl,
    @JsonProperty("prCommitCount") Double prCommitCount
) {
}

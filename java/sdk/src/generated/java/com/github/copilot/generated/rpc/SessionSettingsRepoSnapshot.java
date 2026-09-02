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
    /** Repository name. */
    @JsonProperty("name") String name,
    /** GitHub repository database ID. */
    @JsonProperty("id") Double id,
    /** Checked-out repository branch. */
    @JsonProperty("branch") String branch,
    /** Checked-out commit SHA. */
    @JsonProperty("commit") String commit,
    /** Whether the repository is writable. */
    @JsonProperty("readWrite") Boolean readWrite,
    /** Repository owner login. */
    @JsonProperty("ownerName") String ownerName,
    /** GitHub repository owner database ID. */
    @JsonProperty("ownerId") Double ownerId,
    /** GitHub server base URL. */
    @JsonProperty("serverUrl") String serverUrl,
    /** GitHub server host name. */
    @JsonProperty("host") String host,
    /** Protocol used to access the GitHub host. */
    @JsonProperty("hostProtocol") String hostProtocol,
    /** GitHub secret-scanning service URL. */
    @JsonProperty("secretScanningUrl") String secretScanningUrl,
    /** Number of commits in the pull request. */
    @JsonProperty("prCommitCount") Double prCommitCount
) {
}

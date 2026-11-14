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
 * The release lookup succeeded.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubReleaseSnapshotResultSuccess extends GitHubReleaseSnapshotResult {

    @JsonProperty("outcome")
    private final String outcome = "success";

    @Override
    public String getOutcome() { return outcome; }

    /** The original update channel. */
    @JsonProperty("updateChannel")
    private GitHubReleaseUpdateChannel updateChannel;

    /** The original lookup duration in milliseconds. */
    @JsonProperty("lookupDurationMs")
    private Double lookupDurationMs;

    /** Up to three stable releases, newest first. */
    @JsonProperty("latestStableReleases")
    private List<GitHubReleaseSnapshotEntry> latestStableReleases;

    /** Prereleases newer than the newest stable release, newest first. */
    @JsonProperty("prereleasesNewerThanLatestStable")
    private List<GitHubReleaseSnapshotEntry> prereleasesNewerThanLatestStable;

    /** The effective running version after the existing override rules. */
    @JsonProperty("runningVersion")
    private String runningVersion;

    /** Whether the running release is eligible for release admission. */
    @JsonProperty("eligible")
    private Boolean eligible;

    /** The newest release in the selected update channel, when one exists. */
    @JsonProperty("automaticUpdateCandidate")
    private GitHubReleaseSnapshotEntry automaticUpdateCandidate;

    public GitHubReleaseUpdateChannel getUpdateChannel() { return updateChannel; }
    public void setUpdateChannel(GitHubReleaseUpdateChannel updateChannel) { this.updateChannel = updateChannel; }

    public Double getLookupDurationMs() { return lookupDurationMs; }
    public void setLookupDurationMs(Double lookupDurationMs) { this.lookupDurationMs = lookupDurationMs; }

    public List<GitHubReleaseSnapshotEntry> getLatestStableReleases() { return latestStableReleases; }
    public void setLatestStableReleases(List<GitHubReleaseSnapshotEntry> latestStableReleases) { this.latestStableReleases = latestStableReleases; }

    public List<GitHubReleaseSnapshotEntry> getPrereleasesNewerThanLatestStable() { return prereleasesNewerThanLatestStable; }
    public void setPrereleasesNewerThanLatestStable(List<GitHubReleaseSnapshotEntry> prereleasesNewerThanLatestStable) { this.prereleasesNewerThanLatestStable = prereleasesNewerThanLatestStable; }

    public String getRunningVersion() { return runningVersion; }
    public void setRunningVersion(String runningVersion) { this.runningVersion = runningVersion; }

    public Boolean getEligible() { return eligible; }
    public void setEligible(Boolean eligible) { this.eligible = eligible; }

    public GitHubReleaseSnapshotEntry getAutomaticUpdateCandidate() { return automaticUpdateCandidate; }
    public void setAutomaticUpdateCandidate(GitHubReleaseSnapshotEntry automaticUpdateCandidate) { this.automaticUpdateCandidate = automaticUpdateCandidate; }
}

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
 * The host's machine-wide state. Every field is optional because a fresh install has recorded nothing yet, so a reader must treat an absent field as `not yet`, never as a negative answer. Stored credentials are deliberately absent from this shape.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GlobalStateLoadResult(
    /** Plugins installed on this machine. */
    @JsonProperty("installedPlugins") List<InstalledPlugin> installedPlugins,
    /** Models the user selected recently, most recent first. */
    @JsonProperty("recentModelIds") List<String> recentModelIds,
    /** When the host first ran on this machine. */
    @JsonProperty("firstLaunchAt") String firstLaunchAt,
    /** Terminals the user has already been asked to set up, so the host does not ask twice. */
    @JsonProperty("askedSetupTerminals") List<String> askedSetupTerminals,
    /** Folders where the user declined the init prompt, so it stays hidden there. */
    @JsonProperty("suppressInitFolders") List<String> suppressInitFolders,
    /** Whether the sandbox onboarding has been shown. */
    @JsonProperty("sandboxOnboardingShown") Boolean sandboxOnboardingShown,
    /** Whether the user declined to trust the sandbox credential proxy CA. */
    @JsonProperty("sandboxCredentialProxyCaDeclined") Boolean sandboxCredentialProxyCaDeclined,
    /** Whether the app tip has been shown. */
    @JsonProperty("appTipShown") Boolean appTipShown,
    /** Whether the one-off cleanup of stored reasoning summaries has run. */
    @JsonProperty("reasoningSummariesCleanupDone") Boolean reasoningSummariesCleanupDone,
    /** Account used for the most recent sign-in. */
    @JsonProperty("lastLoggedInUser") LoggedInUser lastLoggedInUser,
    /** Every account the host has signed in to on this machine. */
    @JsonProperty("loggedInUsers") List<LoggedInUser> loggedInUsers,
    /** Whether the user is a GitHub or Microsoft staff member, which unlocks internal-only behavior. */
    @JsonProperty("staff") Boolean staff,
    /** Whether the user was recognized as GitHub staff. */
    @JsonProperty("staffGithub") Boolean staffGitHub,
    /** Whether the user was recognized as Microsoft staff. */
    @JsonProperty("staffMicrosoft") Boolean staffMicrosoft,
    /** When the staff-only model reset last ran. */
    @JsonProperty("staffModelResetAt") String staffModelResetAt,
    /** When the staff-only log level migration last ran. */
    @JsonProperty("staffLogLevelMigrationAt") String staffLogLevelMigrationAt,
    /** When the staff-only update channel migration last ran. */
    @JsonProperty("staffUpdateChannelMigrationAt") String staffUpdateChannelMigrationAt,
    /** Folders the user has marked as trusted. */
    @JsonProperty("trustedFolders") List<String> trustedFolders,
    /** When the Auto-feedback hint was last shown, as an ISO 8601 timestamp. It enforces the once-per-day cap for non-staff users across restarts. */
    @JsonProperty("autoFeedbackLastPromptedAt") String autoFeedbackLastPromptedAt
) {
}

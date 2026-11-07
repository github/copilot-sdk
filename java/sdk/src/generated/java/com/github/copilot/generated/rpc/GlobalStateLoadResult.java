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

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GlobalStateLoadResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private List<InstalledPlugin> installedPlugins;
        private List<String> recentModelIds;
        private String firstLaunchAt;
        private List<String> askedSetupTerminals;
        private List<String> suppressInitFolders;
        private Boolean sandboxOnboardingShown;
        private Boolean sandboxCredentialProxyCaDeclined;
        private Boolean appTipShown;
        private Boolean reasoningSummariesCleanupDone;
        private LoggedInUser lastLoggedInUser;
        private List<LoggedInUser> loggedInUsers;
        private Boolean staff;
        private Boolean staffGitHub;
        private Boolean staffMicrosoft;
        private String staffModelResetAt;
        private String staffLogLevelMigrationAt;
        private String staffUpdateChannelMigrationAt;
        private List<String> trustedFolders;
        private String autoFeedbackLastPromptedAt;

        /**
         * Sets the {@code installedPlugins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedPlugins(List<InstalledPlugin> value) {
            this.installedPlugins = value;
            return this;
        }

        /**
         * Sets the {@code recentModelIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recentModelIds(List<String> value) {
            this.recentModelIds = value;
            return this;
        }

        /**
         * Sets the {@code firstLaunchAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder firstLaunchAt(String value) {
            this.firstLaunchAt = value;
            return this;
        }

        /**
         * Sets the {@code askedSetupTerminals} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder askedSetupTerminals(List<String> value) {
            this.askedSetupTerminals = value;
            return this;
        }

        /**
         * Sets the {@code suppressInitFolders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder suppressInitFolders(List<String> value) {
            this.suppressInitFolders = value;
            return this;
        }

        /**
         * Sets the {@code sandboxOnboardingShown} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxOnboardingShown(Boolean value) {
            this.sandboxOnboardingShown = value;
            return this;
        }

        /**
         * Sets the {@code sandboxCredentialProxyCaDeclined} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sandboxCredentialProxyCaDeclined(Boolean value) {
            this.sandboxCredentialProxyCaDeclined = value;
            return this;
        }

        /**
         * Sets the {@code appTipShown} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder appTipShown(Boolean value) {
            this.appTipShown = value;
            return this;
        }

        /**
         * Sets the {@code reasoningSummariesCleanupDone} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningSummariesCleanupDone(Boolean value) {
            this.reasoningSummariesCleanupDone = value;
            return this;
        }

        /**
         * Sets the {@code lastLoggedInUser} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastLoggedInUser(LoggedInUser value) {
            this.lastLoggedInUser = value;
            return this;
        }

        /**
         * Sets the {@code loggedInUsers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder loggedInUsers(List<LoggedInUser> value) {
            this.loggedInUsers = value;
            return this;
        }

        /**
         * Sets the {@code staff} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staff(Boolean value) {
            this.staff = value;
            return this;
        }

        /**
         * Sets the {@code staffGithub} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staffGitHub(Boolean value) {
            this.staffGitHub = value;
            return this;
        }

        /**
         * Sets the {@code staffMicrosoft} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staffMicrosoft(Boolean value) {
            this.staffMicrosoft = value;
            return this;
        }

        /**
         * Sets the {@code staffModelResetAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staffModelResetAt(String value) {
            this.staffModelResetAt = value;
            return this;
        }

        /**
         * Sets the {@code staffLogLevelMigrationAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staffLogLevelMigrationAt(String value) {
            this.staffLogLevelMigrationAt = value;
            return this;
        }

        /**
         * Sets the {@code staffUpdateChannelMigrationAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staffUpdateChannelMigrationAt(String value) {
            this.staffUpdateChannelMigrationAt = value;
            return this;
        }

        /**
         * Sets the {@code trustedFolders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder trustedFolders(List<String> value) {
            this.trustedFolders = value;
            return this;
        }

        /**
         * Sets the {@code autoFeedbackLastPromptedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoFeedbackLastPromptedAt(String value) {
            this.autoFeedbackLastPromptedAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GlobalStateLoadResult build() {
            return new GlobalStateLoadResult(installedPlugins, recentModelIds, firstLaunchAt, askedSetupTerminals, suppressInitFolders, sandboxOnboardingShown, sandboxCredentialProxyCaDeclined, appTipShown, reasoningSummariesCleanupDone, lastLoggedInUser, loggedInUsers, staff, staffGitHub, staffMicrosoft, staffModelResetAt, staffLogLevelMigrationAt, staffUpdateChannelMigrationAt, trustedFolders, autoFeedbackLastPromptedAt);
        }
    }
}

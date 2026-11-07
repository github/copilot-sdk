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
 * Workspace diff result for the requested mode.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesDiffResult(
    /** Diff mode requested by the client. */
    @JsonProperty("requestedMode") WorkspaceDiffMode requestedMode,
    /** Effective mode used for the returned changes. */
    @JsonProperty("mode") WorkspaceDiffMode mode,
    /** Changed files and their unified diffs. */
    @JsonProperty("changes") List<WorkspaceDiffFileChange> changes,
    /** Default branch used for a branch diff, when branch mode was requested. */
    @JsonProperty("baseBranch") String baseBranch,
    /** Whether the requested diff fell back to unstaged changes, either because branch diff failed or session diff was unavailable. */
    @JsonProperty("isFallback") Boolean isFallback,
    /** Why the session diff could not be produced, when applicable. Set only when `session` mode was requested and `isFallback` is true, so a client can tell the permanent `file-change-tracking-disabled` apart from the transient `session-busy`, which the same request answers once the session settles. Never set for `unstaged` or `branch` mode, and never `unsupported-remote-session`: a remote session's captures live on its own host, so a `session`-mode diff is rejected for one rather than answered with a controller-side fallback. */
    @JsonProperty("unavailableReason") HistoryRewindUnavailableReason unavailableReason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkspacesDiffResult {}

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
        private WorkspaceDiffMode requestedMode;
        private WorkspaceDiffMode mode;
        private List<WorkspaceDiffFileChange> changes;
        private String baseBranch;
        private Boolean isFallback;
        private HistoryRewindUnavailableReason unavailableReason;

        /**
         * Sets the {@code requestedMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestedMode(WorkspaceDiffMode value) {
            this.requestedMode = value;
            return this;
        }

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mode(WorkspaceDiffMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code changes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder changes(List<WorkspaceDiffFileChange> value) {
            this.changes = value;
            return this;
        }

        /**
         * Sets the {@code baseBranch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder baseBranch(String value) {
            this.baseBranch = value;
            return this;
        }

        /**
         * Sets the {@code isFallback} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isFallback(Boolean value) {
            this.isFallback = value;
            return this;
        }

        /**
         * Sets the {@code unavailableReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder unavailableReason(HistoryRewindUnavailableReason value) {
            this.unavailableReason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkspacesDiffResult build() {
            if (requestedMode == null) throw new IllegalStateException("Missing required property: requestedMode");
            if (mode == null) throw new IllegalStateException("Missing required property: mode");
            if (changes == null) throw new IllegalStateException("Missing required property: changes");
            if (isFallback == null) throw new IllegalStateException("Missing required property: isFallback");
            return new SessionWorkspacesDiffResult(requestedMode, mode, changes, baseBranch, isFallback, unavailableReason);
        }
    }
}

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
 * Rewind points and file-change-tracking availability for the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryListRewindPointsResult(
    /** Whether this session captured file changes from its first turn. */
    @JsonProperty("fileChangeTrackingEnabled") Boolean fileChangeTrackingEnabled,
    /** Why the listed points could not be produced, when applicable; the points list is empty whenever it is set. `unsupported-remote-session` is permanent for the session and comes with `fileChangeTrackingEnabled: false`. `session-busy` is transient and only ever reported by a session that *is* tracking (`fileChangeTrackingEnabled: true`), because the file-change captures cannot be read while work that may still mutate them is in flight; the same request succeeds once the session settles, so a client that wants points should retry rather than treat it as a failure. It is never `file-change-tracking-disabled`: an untracked local session still lists conversation-only points and reports that through `fileChangeTrackingEnabled: false`. */
    @JsonProperty("unavailableReason") HistoryRewindUnavailableReason unavailableReason,
    /** Root user turns in chronological order. Empty when `unavailableReason` is set. */
    @JsonProperty("points") List<HistoryRewindPoint> points
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionHistoryListRewindPointsResult {}

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
        private Boolean fileChangeTrackingEnabled;
        private HistoryRewindUnavailableReason unavailableReason;
        private List<HistoryRewindPoint> points;

        /**
         * Sets the {@code fileChangeTrackingEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fileChangeTrackingEnabled(Boolean value) {
            this.fileChangeTrackingEnabled = value;
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
         * Sets the {@code points} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder points(List<HistoryRewindPoint> value) {
            this.points = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionHistoryListRewindPointsResult build() {
            if (fileChangeTrackingEnabled == null) throw new IllegalStateException("Missing required property: fileChangeTrackingEnabled");
            if (points == null) throw new IllegalStateException("Missing required property: points");
            return new SessionHistoryListRewindPointsResult(fileChangeTrackingEnabled, unavailableReason, points);
        }
    }
}

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
 * A cloud task's recorded events, ordered for replay. Read `preStart` first: it selects which of the two event lists a caller should use, and they differ deliberately.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksGetSessionReplayResult(
    /** The events to replay in order to reconstruct the session, as a JSON array. Empty when `preStart` is true, because a session that has not started has nothing to reconstruct and replaying its pending events would invent a history it never had. */
    @JsonProperty("replayEventsJson") String replayEventsJson,
    /** Every recorded event in the same replay order, as a JSON array, whether or not the session has started. This is the list to hand to a subsequent ingress so it can tell which events it has already seen, and unlike `replayEventsJson` it is never emptied. */
    @JsonProperty("observedEventsJson") String observedEventsJson,
    /** True when the task has recorded events but the session has not started yet, so only queued work exists. A started session is anchored by either `session.start` or, for a resumed one, `session.resume`. */
    @JsonProperty("preStart") Boolean preStart
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksGetSessionReplayResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String replayEventsJson;
        private String observedEventsJson;
        private Boolean preStart;

        /**
         * Sets the {@code replayEventsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder replayEventsJson(String value) {
            this.replayEventsJson = value;
            return this;
        }

        /**
         * Sets the {@code observedEventsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder observedEventsJson(String value) {
            this.observedEventsJson = value;
            return this;
        }

        /**
         * Sets the {@code preStart} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder preStart(Boolean value) {
            this.preStart = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CloudTasksGetSessionReplayResult build() {
            if (replayEventsJson == null) throw new IllegalStateException("Missing required property: replayEventsJson");
            if (observedEventsJson == null) throw new IllegalStateException("Missing required property: observedEventsJson");
            if (preStart == null) throw new IllegalStateException("Missing required property: preStart");
            return new CloudTasksGetSessionReplayResult(replayEventsJson, observedEventsJson, preStart);
        }
    }
}

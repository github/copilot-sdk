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
 * Request parameters for the {@code session.history.compact} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryCompactParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Optional user-provided instructions to focus the compaction summary */
    @JsonProperty("customInstructions") String customInstructions,
    /** What initiated this compaction request, recorded as the `trigger` on the persisted `session.compaction_start` / `session.compaction_complete` events. When absent, the compaction is persisted without trigger attribution (initiator unknown). */
    @JsonProperty("trigger") SessionHistoryCompactParamsTrigger trigger,
    /** Context window token limit this compaction is targeting, recorded as the `tokenLimit` on the persisted `session.compaction_start` / `session.compaction_complete` events. Set it when the compaction targets a window other than the compacting model's own, e.g. switching to a model with a smaller context window: the compaction still runs on the current model, so the limit that motivated it would otherwise be lost. When absent, the events record the compacting model's own resolved limit. Attribution metadata only - it does not change how much the compaction removes. */
    @JsonProperty("tokenLimit") Long tokenLimit
) {

    /** What initiated this compaction request, recorded as the `trigger` on the persisted `session.compaction_start` / `session.compaction_complete` events. When absent, the compaction is persisted without trigger attribution (initiator unknown). */
    public enum SessionHistoryCompactParamsTrigger {
        /** The {@code manual} variant. */
        MANUAL("manual"),
        /** The {@code model_switch} variant. */
        MODEL_SWITCH("model_switch");

        private final String value;
        SessionHistoryCompactParamsTrigger(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static SessionHistoryCompactParamsTrigger fromValue(String value) {
            for (SessionHistoryCompactParamsTrigger v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown SessionHistoryCompactParamsTrigger value: " + value);
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionHistoryCompactParams {}

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
        private String sessionId;
        private String customInstructions;
        private SessionHistoryCompactParamsTrigger trigger;
        private Long tokenLimit;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code customInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder customInstructions(String value) {
            this.customInstructions = value;
            return this;
        }

        /**
         * Sets the {@code trigger} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder trigger(SessionHistoryCompactParamsTrigger value) {
            this.trigger = value;
            return this;
        }

        /**
         * Sets the {@code tokenLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenLimit(Long value) {
            this.tokenLimit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionHistoryCompactParams build() {
            return new SessionHistoryCompactParams(sessionId, customInstructions, trigger, tokenLimit);
        }
    }
}

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
}

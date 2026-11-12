/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "instruction.update". An authoritative developer-instruction update. Event order defines instruction order. Persistent updates are durable and restore across replay, resume, rewind, fork, and compaction; turn-scoped updates must be emitted with ephemeral=true and are never restored.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class InstructionUpdateEvent extends SessionEvent {

    @Override
    public String getType() { return "instruction.update"; }

    @JsonProperty("data")
    private InstructionUpdateEventData data;

    public InstructionUpdateEventData getData() { return data; }
    public void setData(InstructionUpdateEventData data) { this.data = data; }

    /** Data payload for {@link InstructionUpdateEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InstructionUpdateEventData(
        /** Authoritative instruction text. Empty or whitespace-only content is rejected. */
        @JsonProperty("content") String content,
        /** Authority responsible for the update. */
        @JsonProperty("authority") InstructionUpdateAuthority authority,
        /** Whether the update persists beyond the current turn. */
        @JsonProperty("lifetime") InstructionUpdateLifetime lifetime,
        /** Earliest legal insertion boundary. */
        @JsonProperty("boundary") InstructionUpdateBoundary boundary,
        /** Deterministic behavior when the active provider cannot represent the update. */
        @JsonProperty("fallback") InstructionUpdateFallback fallback,
        /** Optional stable source name for diagnostics and future host-authored updates. */
        @JsonProperty("name") String name,
        /** Whether this update was recorded after the conversation's initial user message. */
        @JsonProperty("postInitial") Boolean postInitial
    ) {
    }
}

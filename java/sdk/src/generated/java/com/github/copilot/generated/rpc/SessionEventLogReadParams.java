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
 * Cursor, batch size, and optional long-poll/filter parameters for reading session events.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionEventLogReadParams.$NullableDeserializer.class)
public record SessionEventLogReadParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque cursor returned by a previous read in the same view. Omit on the first call to start from the beginning of the selected view. */
    @JsonProperty("cursor") String cursor,
    /** Maximum number of events to return in this batch (1–1000, default 200). */
    @JsonProperty("max") Long max,
    /** Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur. */
    @JsonProperty("waitMs") Long waitMs,
    /** Either '*' to receive all event types, or a non-empty list of event types to receive */
    @JsonProperty("types") Object types,
    /** Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers. */
    @JsonProperty("agentScope") EventsAgentScope agentScope,
    /** Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope. */
    @JsonProperty("agentIds") List<String> agentIds,
    /** Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion. */
    @JsonProperty("direction") EventsReadDirection direction,
    /** When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only. */
    @JsonProperty("includeEphemeral") Boolean includeEphemeral,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionEventLogReadParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionEventLogReadParams(String sessionId, String cursor, Long max, Long waitMs, Object types, EventsAgentScope agentScope, List<String> agentIds, EventsReadDirection direction, Boolean includeEphemeral) {
        this(sessionId, cursor, max, waitMs, types, agentScope, agentIds, direction, includeEphemeral, java.util.Set.of());
    }

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
        private String sessionId;
        private String cursor;
        private Long max;
        private Long waitMs;
        private Object types;
        private boolean $presence4;
        private EventsAgentScope agentScope;
        private List<String> agentIds;
        private EventsReadDirection direction;
        private Boolean includeEphemeral;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code cursor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cursor(String value) {
            this.cursor = value;
            return this;
        }

        /**
         * Sets the {@code max} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder max(Long value) {
            this.max = value;
            return this;
        }

        /**
         * Sets the {@code waitMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder waitMs(Long value) {
            this.waitMs = value;
            return this;
        }

        /**
         * Sets the {@code types} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder types(Object value) {
            this.types = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Sets the {@code agentScope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder agentScope(EventsAgentScope value) {
            this.agentScope = value;
            return this;
        }

        /**
         * Sets the {@code agentIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder agentIds(List<String> value) {
            this.agentIds = value;
            return this;
        }

        /**
         * Sets the {@code direction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder direction(EventsReadDirection value) {
            this.direction = value;
            return this;
        }

        /**
         * Sets the {@code includeEphemeral} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder includeEphemeral(Boolean value) {
            this.includeEphemeral = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionEventLogReadParams build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence4 && types == null) $explicitNulls.add("types");
            return new SessionEventLogReadParams(sessionId, cursor, max, waitMs, types, agentScope, agentIds, direction, includeEphemeral, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("types") && types == null) values.put("types", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionEventLogReadParams> {
        @Override
        public SessionEventLogReadParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionEventLogReadParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("types") && tree.get("types").isNull()) $explicitNulls.add("types");
            return new SessionEventLogReadParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "cursor", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "max", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "waitMs", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "types", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "agentScope", new com.fasterxml.jackson.core.type.TypeReference<EventsAgentScope>() {}), readProperty(context, tree, "agentIds", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "direction", new com.fasterxml.jackson.core.type.TypeReference<EventsReadDirection>() {}), readProperty(context, tree, "includeEphemeral", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}

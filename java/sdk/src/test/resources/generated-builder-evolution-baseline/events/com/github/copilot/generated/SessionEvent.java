/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Base class for all generated session events.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UnknownSessionEvent.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SampleUpdatedEvent.class, name = "sample.updated")
})
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract sealed class SessionEvent permits
        SampleUpdatedEvent,
        UnknownSessionEvent {

    /** Unique event identifier (UUID v4), generated when the event is emitted. */
    @JsonProperty("id")
    private UUID id;

    /** ISO 8601 timestamp when the event was created. */
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    /** ID of the chronologically preceding event in the session. Null for the first event. */
    @JsonProperty("parentId")
    private UUID parentId;

    /** Sub-agent instance identifier. Absent for events from the root/main agent and session-level events. */
    @JsonProperty("agentId")
    private String agentId;

    /** When true, the event is transient and not persisted to the session event log on disk. */
    @JsonProperty("ephemeral")
    private Boolean ephemeral;

    /**
     * Returns the event-type discriminator string (e.g., {@code "session.idle"}).
     *
     * @return the event type
     */
    public abstract String getType();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public Boolean getEphemeral() { return ephemeral; }
    public void setEphemeral(Boolean ephemeral) { this.ephemeral = ephemeral; }
}

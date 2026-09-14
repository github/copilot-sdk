/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.messageAuthorization". Freezes one blinded, verbatim-verified authorization claim the runtime minted from a human user message, so a resumed session re-establishes the same grant deterministically instead of re-running the extraction model. This mints no authority on its own: it records what a blinded proposer pointed at and the trusted discriminator the runtime established, and deterministic establishment runs on replay. Persisted so recorded authority survives compaction and process resume.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionMessageAuthorizationEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.messageAuthorization"; }

    @JsonProperty("data")
    private PermissionMessageAuthorizationEventData data;

    public PermissionMessageAuthorizationEventData getData() { return data; }
    public void setData(PermissionMessageAuthorizationEventData data) { this.data = data; }

    /** Data payload for {@link PermissionMessageAuthorizationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionMessageAuthorizationEventData(
        /** Deterministic identity of the record, derived from the turn and span offsets so re-extracting the same span mints nothing new. */
        @JsonProperty("recordId") String recordId,
        /** The human turn the quoted span was read from. */
        @JsonProperty("turnIndex") Long turnIndex,
        /** Whether the claim granted or denied authority. */
        @JsonProperty("polarity") PermissionMessageAuthorizationPolarity polarity,
        /** The kind of effect authorized, as an action-class identifier. */
        @JsonProperty("actionClass") String actionClass,
        /** Start byte offset of the authorizing span within the turn. */
        @JsonProperty("spanStart") Long spanStart,
        /** End byte offset of the authorizing span within the turn. */
        @JsonProperty("spanEnd") Long spanEnd,
        /** Concrete named targets that appear verbatim inside the span. */
        @JsonProperty("targetMembers") List<String> targetMembers,
        /** The task the permission is scoped to, when the human named one. */
        @JsonProperty("task") String task,
        /** The trusted version discriminator, when one exists. Exact shell-command grants carry the byte-identical commands grounded in the human span; world-derived classes carry a file object, remote tip, or runner only when that state was captured safely. An opaque object mirroring the runtime's adjacently-tagged resolution. */
        @JsonProperty("world") Object world
    ) {
    }
}

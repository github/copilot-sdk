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
 * Session event "permission.contextualAuthorization". Historical decode-only contextual authorization claim. Current runtimes preserve the payload but do not establish authority from it.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionContextualAuthorizationEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.contextualAuthorization"; }

    @JsonProperty("data")
    private PermissionContextualAuthorizationEventData data;

    public PermissionContextualAuthorizationEventData getData() { return data; }
    public void setData(PermissionContextualAuthorizationEventData data) { this.data = data; }

    /** Data payload for {@link PermissionContextualAuthorizationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionContextualAuthorizationEventData(
        /** Deterministic identity of the contextual message grant. */
        @JsonProperty("recordId") String recordId,
        /** Original blocked permission request selected by deterministic event ordering, never by the extraction model. */
        @JsonProperty("requestId") String requestId,
        /** Human turn containing the contextual decision. */
        @JsonProperty("turnIndex") Long turnIndex,
        /** Whether the contextual human span granted or denied authority. */
        @JsonProperty("polarity") PermissionMessageAuthorizationPolarity polarity,
        /** Start byte offset of the contextual decision span within the turn. */
        @JsonProperty("spanStart") Long spanStart,
        /** End byte offset of the contextual decision span within the turn. */
        @JsonProperty("spanEnd") Long spanEnd
    ) {
    }
}

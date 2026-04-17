/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code permission.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.completed"; }

    @JsonProperty("data")
    private PermissionCompletedEventData data;

    public PermissionCompletedEventData getData() { return data; }
    public void setData(PermissionCompletedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionCompletedEventData(
        /** Request ID of the resolved permission request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** The result of the permission request */
        @JsonProperty("result") PermissionCompletedEventDataResult result
    ) {

        /** The result of the permission request */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record PermissionCompletedEventDataResult(
            /** The outcome of the permission request */
            @JsonProperty("kind") PermissionCompletedEventDataResultKind kind
        ) {

            /** The outcome of the permission request */
            public enum PermissionCompletedEventDataResultKind {
                /** The {@code approved} variant. */
                APPROVED("approved"),
                /** The {@code denied-by-rules} variant. */
                DENIED_BY_RULES("denied-by-rules"),
                /** The {@code denied-no-approval-rule-and-could-not-request-from-user} variant. */
                DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER("denied-no-approval-rule-and-could-not-request-from-user"),
                /** The {@code denied-interactively-by-user} variant. */
                DENIED_INTERACTIVELY_BY_USER("denied-interactively-by-user"),
                /** The {@code denied-by-content-exclusion-policy} variant. */
                DENIED_BY_CONTENT_EXCLUSION_POLICY("denied-by-content-exclusion-policy"),
                /** The {@code denied-by-permission-request-hook} variant. */
                DENIED_BY_PERMISSION_REQUEST_HOOK("denied-by-permission-request-hook");

                private final String value;
                PermissionCompletedEventDataResultKind(String value) { this.value = value; }
                @com.fasterxml.jackson.annotation.JsonValue
                public String getValue() { return value; }
                @com.fasterxml.jackson.annotation.JsonCreator
                public static PermissionCompletedEventDataResultKind fromValue(String value) {
                    for (PermissionCompletedEventDataResultKind v : values()) {
                        if (v.value.equals(value)) return v;
                    }
                    throw new IllegalArgumentException("Unknown PermissionCompletedEventDataResultKind value: " + value);
                }
            }
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.mode.set} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModeSetParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The mode to switch to. Valid values: "interactive", "plan", "autopilot". */
    @JsonProperty("mode") SessionModeSetParamsMode mode
) {

    /** The mode to switch to. Valid values: "interactive", "plan", "autopilot". */
    public enum SessionModeSetParamsMode {
        /** The {@code interactive} variant. */
        INTERACTIVE("interactive"),
        /** The {@code plan} variant. */
        PLAN("plan"),
        /** The {@code autopilot} variant. */
        AUTOPILOT("autopilot");

        private final String value;
        SessionModeSetParamsMode(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static SessionModeSetParamsMode fromValue(String value) {
            for (SessionModeSetParamsMode v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown SessionModeSetParamsMode value: " + value);
        }
    }
}

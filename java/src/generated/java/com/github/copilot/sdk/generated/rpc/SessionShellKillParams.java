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
 * Request parameters for the {@code session.shell.kill} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionShellKillParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Process identifier returned by shell.exec */
    @JsonProperty("processId") String processId,
    /** Signal to send (default: SIGTERM) */
    @JsonProperty("signal") SessionShellKillParamsSignal signal
) {

    /** Signal to send (default: SIGTERM) */
    public enum SessionShellKillParamsSignal {
        /** The {@code SIGTERM} variant. */
        SIGTERM("SIGTERM"),
        /** The {@code SIGKILL} variant. */
        SIGKILL("SIGKILL"),
        /** The {@code SIGINT} variant. */
        SIGINT("SIGINT");

        private final String value;
        SessionShellKillParamsSignal(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static SessionShellKillParamsSignal fromValue(String value) {
            for (SessionShellKillParamsSignal v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown SessionShellKillParamsSignal value: " + value);
        }
    }
}

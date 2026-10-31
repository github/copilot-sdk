/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Runtime state a host waits for while it executes a queued command.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum QueuedCommandWaitRequirement {
    /** The {@code managed-policy-published} variant. */
    MANAGED_POLICY_PUBLISHED("managed-policy-published");

    private final String value;
    QueuedCommandWaitRequirement(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static QueuedCommandWaitRequirement fromValue(String value) {
        for (QueuedCommandWaitRequirement v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown QueuedCommandWaitRequirement value: " + value);
    }
}

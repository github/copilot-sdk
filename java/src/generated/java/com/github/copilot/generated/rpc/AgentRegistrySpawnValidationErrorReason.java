/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistrySpawnValidationErrorReason {
    /** The {@code cwd-not-found} variant. */
    CWD_NOT_FOUND("cwd-not-found"),
    /** The {@code cwd-not-directory} variant. */
    CWD_NOT_DIRECTORY("cwd-not-directory"),
    /** The {@code invalid-name} variant. */
    INVALID_NAME("invalid-name"),
    /** The {@code unknown-agent} variant. */
    UNKNOWN_AGENT("unknown-agent"),
    /** The {@code unknown-model} variant. */
    UNKNOWN_MODEL("unknown-model"),
    /** The {@code yolo-not-allowed} variant. */
    YOLO_NOT_ALLOWED("yolo-not-allowed");

    private final String value;
    AgentRegistrySpawnValidationErrorReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistrySpawnValidationErrorReason fromValue(String value) {
        for (AgentRegistrySpawnValidationErrorReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistrySpawnValidationErrorReason value: " + value);
    }
}

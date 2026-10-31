/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The cached host error-reporting policy. Pending means startup policy has not resolved. This does not describe network delivery.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HostErrorReportingStatus {
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code enabled} variant. */
    ENABLED("enabled"),
    /** The {@code disabled} variant. */
    DISABLED("disabled");

    private final String value;
    HostErrorReportingStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HostErrorReportingStatus fromValue(String value) {
        for (HostErrorReportingStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HostErrorReportingStatus value: " + value);
    }
}

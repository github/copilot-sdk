/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Session capability enabled for this session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionCapability {
    /** The {@code tui-hints} variant. */
    TUI_HINTS("tui-hints"),
    /** The {@code plan-mode} variant. */
    PLAN_MODE("plan-mode"),
    /** The {@code memory} variant. */
    MEMORY("memory"),
    /** The {@code cli-documentation} variant. */
    CLI_DOCUMENTATION("cli-documentation"),
    /** The {@code ask-user} variant. */
    ASK_USER("ask-user"),
    /** The {@code interactive-mode} variant. */
    INTERACTIVE_MODE("interactive-mode"),
    /** The {@code system-notifications} variant. */
    SYSTEM_NOTIFICATIONS("system-notifications"),
    /** The {@code elicitation} variant. */
    ELICITATION("elicitation"),
    /** The {@code session-store} variant. */
    SESSION_STORE("session-store"),
    /** The {@code mcp-apps} variant. */
    MCP_APPS("mcp-apps"),
    /** The {@code canvas-renderer} variant. */
    CANVAS_RENDERER("canvas-renderer");

    private final String value;
    SessionCapability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionCapability fromValue(String value) {
        for (SessionCapability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionCapability value: " + value);
    }
}

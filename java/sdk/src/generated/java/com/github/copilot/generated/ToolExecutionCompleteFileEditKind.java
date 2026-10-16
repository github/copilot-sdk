/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Kind of file mutation committed by a built-in editing tool.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteFileEditKind {
    /** The {@code create} variant. */
    public static final ToolExecutionCompleteFileEditKind CREATE = new ToolExecutionCompleteFileEditKind("create");
    /** The {@code edit} variant. */
    public static final ToolExecutionCompleteFileEditKind EDIT = new ToolExecutionCompleteFileEditKind("edit");
    /** The {@code delete} variant. */
    public static final ToolExecutionCompleteFileEditKind DELETE = new ToolExecutionCompleteFileEditKind("delete");
    /** An explicit unknown file operation kind. */
    public static final ToolExecutionCompleteFileEditKind UNKNOWN = new ToolExecutionCompleteFileEditKind("unknown");

    private final String value;
    private ToolExecutionCompleteFileEditKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ToolExecutionCompleteFileEditKind fromValue(String value) {
        if (value == null) throw new IllegalArgumentException("Missing ToolExecutionCompleteFileEditKind value");
        if ("create".equals(value)) return CREATE;
        if ("edit".equals(value)) return EDIT;
        if ("delete".equals(value)) return DELETE;
        if ("unknown".equals(value)) return UNKNOWN;
        return new ToolExecutionCompleteFileEditKind(value);
    }
    @Override
    public boolean equals(Object other) {
        return other instanceof ToolExecutionCompleteFileEditKind kind && value.equals(kind.value);
    }
    @Override
    public int hashCode() { return value.hashCode(); }
    @Override
    public String toString() { return value; }
}

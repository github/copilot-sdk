/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What kind of resource a catalog candidate describes
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogCandidateKind {
    /** The {@code mcp-server} variant. */
    MCP_SERVER("mcp-server"),
    /** The {@code ai-skill} variant. */
    AI_SKILL("ai-skill");

    private final String value;
    CatalogCandidateKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogCandidateKind fromValue(String value) {
        for (CatalogCandidateKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogCandidateKind value: " + value);
    }
}

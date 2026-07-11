/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output for a pre-MCP-tool-call hook.
 * <p>
 * The {@link #metaToUse} property controls outgoing MCP request metadata:
 * <ul>
 * <li>Return {@code null} from the hook handler: preserve existing
 * {@code _meta} (no-op).</li>
 * <li>Return a {@code PreMcpToolCallHookOutput} with {@code metaToUse} left as
 * {@code null}: remove {@code _meta} from the request.</li>
 * <li>Return a {@code PreMcpToolCallHookOutput} with {@code metaToUse} set to a
 * JSON object: replace {@code _meta} with that object.</li>
 * </ul>
 *
 * @since 1.0.8
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PreMcpToolCallHookOutput {

    @JsonProperty("metaToUse")
    private JsonNode metaToUse;

    /**
     * Gets the metadata to use for the outgoing MCP request.
     *
     * @return the metadata JSON node, or {@code null} to remove metadata
     */
    public JsonNode getMetaToUse() {
        return metaToUse;
    }

    /**
     * Sets the metadata to use for the outgoing MCP request.
     *
     * @param metaToUse
     *            the metadata JSON node, or {@code null} to remove metadata
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookOutput setMetaToUse(JsonNode metaToUse) {
        this.metaToUse = metaToUse;
        return this;
    }

    /**
     * Creates a hook output that sets the given metadata on the MCP request.
     *
     * @param metaToUse
     *            the metadata JSON node to use
     * @return the hook output
     */
    public static PreMcpToolCallHookOutput withMeta(JsonNode metaToUse) {
        return new PreMcpToolCallHookOutput().setMetaToUse(metaToUse);
    }

    /**
     * Creates a hook output that removes metadata from the MCP request.
     *
     * @return the hook output with {@code null} metaToUse
     */
    public static PreMcpToolCallHookOutput removeMeta() {
        return new PreMcpToolCallHookOutput();
    }
}

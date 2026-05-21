/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output for a pre-MCP-tool-call hook.
 * <p>
 * Controls the {@code _meta} field sent with the MCP tool call:
 * <ul>
 * <li>Return {@code null} from the handler to preserve the existing
 * {@code _meta} unchanged.</li>
 * <li>Return {@code new PreMcpToolCallHookOutput(null)} to remove
 * {@code _meta}.</li>
 * <li>Return {@code new PreMcpToolCallHookOutput(metaNode)} to replace
 * {@code _meta} with the provided value.</li>
 * </ul>
 *
 * @param metaToUse
 *            the meta value to use; {@code null} means remove {@code _meta}
 * @since 1.0.8
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PreMcpToolCallHookOutput(@JsonProperty("metaToUse") JsonNode metaToUse) {
}

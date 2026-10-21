/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;

/**
 * Namespace grouping related {@link ToolDefinition}s for model tool discovery.
 * <p>
 * Where the model provider supports native tool namespaces, deferred tools
 * sharing a namespace are presented to the model as one group; elsewhere the
 * tools stay flat. Each tool keeps its unique name either way.
 *
 * @param name
 *            the namespace name: 1-64 ASCII letters, digits, underscores, or
 *            hyphens
 * @param description
 *            an optional model-visible explanation of the tools in this
 *            namespace; {@code null} when unset
 * @see ToolDefinition#namespace(ToolNamespace)
 * @since 1.0.15-preview.1
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolNamespace(@JsonProperty("name") String name, @JsonProperty("description") String description) {

    /**
     * Creates a namespace without a description.
     *
     * @param name
     *            the namespace name
     */
    public ToolNamespace(String name) {
        this(name, null);
    }
}

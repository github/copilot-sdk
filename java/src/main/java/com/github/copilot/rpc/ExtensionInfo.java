/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;

/**
 * Stable extension identity for session participants that provide extension
 * capabilities.
 *
 * @since 1.5.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtensionInfo {

    @JsonProperty("source")
    private String source;

    @JsonProperty("name")
    private String name;

    /**
     * Creates an empty extension info object.
     */
    public ExtensionInfo() {
    }

    /**
     * Creates extension identity metadata.
     *
     * @param source
     *            extension namespace/source, for example {@code "github-app"}
     * @param name
     *            stable provider name within the source namespace
     */
    public ExtensionInfo(String source, String name) {
        this.source = source;
        this.name = name;
    }

    /**
     * Gets the extension namespace/source.
     *
     * @return the extension source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the extension namespace/source.
     *
     * @param source
     *            the extension source
     * @return this instance for method chaining
     */
    public ExtensionInfo setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Gets the stable provider name within the source namespace.
     *
     * @return the extension name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the stable provider name within the source namespace.
     *
     * @param name
     *            the extension name
     * @return this instance for method chaining
     */
    public ExtensionInfo setName(String name) {
        this.name = name;
        return this;
    }
}

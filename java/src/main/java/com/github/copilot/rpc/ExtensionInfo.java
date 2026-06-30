/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * Stable extension identity for session participants that provide canvases.
 * <p>
 * Required when {@link SessionConfig#setCanvases(java.util.List) canvases} are
 * declared so the runtime can attribute the declared canvases back to this
 * provider. All setter methods return {@code this} for method chaining.
 * <p>
 * <strong>Experimental.</strong> Canvas configuration is part of an
 * experimental wire-protocol surface and may change or be removed in future SDK
 * or CLI releases.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var info = new ExtensionInfo().setSource("github-app").setName("canvas-provider");
 * }</pre>
 *
 * @see SessionConfig#setExtensionInfo(ExtensionInfo)
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtensionInfo {

    @JsonProperty("source")
    private String source;

    @JsonProperty("name")
    private String name;

    /**
     * Creates an empty extension identity.
     */
    public ExtensionInfo() {
    }

    /**
     * Creates an extension identity with the given source and name.
     *
     * @param source
     *            the extension namespace/source, e.g. {@code "github-app"}
     * @param name
     *            the stable provider name within the source namespace
     */
    public ExtensionInfo(String source, String name) {
        this.source = source;
        this.name = name;
    }

    /**
     * Gets the extension namespace/source.
     *
     * @return the extension source, e.g. {@code "github-app"}
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the extension namespace/source, e.g. {@code "github-app"}.
     *
     * @param source
     *            the extension source
     * @return this identity for method chaining
     */
    public ExtensionInfo setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Gets the stable provider name within the source namespace.
     *
     * @return the provider name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the stable provider name within the source namespace.
     *
     * @param name
     *            the provider name
     * @return this identity for method chaining
     */
    public ExtensionInfo setName(String name) {
        this.name = name;
        return this;
    }
}

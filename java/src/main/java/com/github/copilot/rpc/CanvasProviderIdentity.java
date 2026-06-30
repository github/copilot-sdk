/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * Session-level identity for the participant that provides canvases on this
 * connection.
 * <p>
 * Supplied as the optional {@code canvasProvider} field on session creation and
 * resume. The {@link #getId() id} is opaque and used verbatim as the canvas
 * {@code extensionId}; a value such as {@code "app:builtin:<windowId>"} is
 * recommended. The {@link #getName() name} is an optional display name. All
 * setter methods return {@code this} for method chaining.
 * <p>
 * <strong>Experimental.</strong> Canvas configuration is part of an
 * experimental wire-protocol surface and may change or be removed in future SDK
 * or CLI releases.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var provider = new CanvasProviderIdentity().setId("app:builtin:main").setName("My App");
 * }</pre>
 *
 * @see SessionConfig#setCanvasProvider(CanvasProviderIdentity)
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanvasProviderIdentity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    /**
     * Creates an empty canvas provider identity.
     */
    public CanvasProviderIdentity() {
    }

    /**
     * Creates a canvas provider identity with the given id.
     *
     * @param id
     *            the opaque provider identifier used verbatim as the canvas
     *            {@code extensionId}
     */
    public CanvasProviderIdentity(String id) {
        this.id = id;
    }

    /**
     * Gets the opaque provider identifier.
     *
     * @return the provider id, used verbatim as the canvas {@code extensionId}
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the opaque provider identifier, used verbatim as the canvas
     * {@code extensionId}. A value such as {@code "app:builtin:<windowId>"} is
     * recommended.
     *
     * @param id
     *            the provider id
     * @return this identity for method chaining
     */
    public CanvasProviderIdentity setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Gets the optional display name.
     *
     * @return the display name, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the optional display name for this provider.
     *
     * @param name
     *            the display name
     * @return this identity for method chaining
     */
    public CanvasProviderIdentity setName(String name) {
        this.name = name;
        return this;
    }
}

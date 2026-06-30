/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.CanvasAction;

/**
 * Declarative metadata for a single canvas, sent over the wire on
 * {@code session.create} / {@code session.resume}.
 * <p>
 * The runtime advertises declared canvases to the agent and routes inbound
 * {@code canvas.open} / {@code canvas.close} / {@code canvas.action.invoke}
 * requests for any declared canvas to the session's {@link CanvasHandler}.
 * Install a handler via {@link SessionConfig#setCanvasHandler(CanvasHandler)}
 * and identify the provider via
 * {@link SessionConfig#setExtensionInfo(ExtensionInfo)}. All setter methods
 * return {@code this} for method chaining.
 * <p>
 * <strong>Experimental.</strong> Canvas configuration is part of an
 * experimental wire-protocol surface and may change or be removed in future SDK
 * or CLI releases.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var canvas = new CanvasDeclaration().setId("counter").setDisplayName("Counter")
 * 		.setDescription("Tracks a counter value.")
 * 		.setActions(List.of(new CanvasAction("increment", "Increments the counter.", null)));
 * }</pre>
 *
 * @see SessionConfig#setCanvases(List)
 * @see CanvasHandler
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanvasDeclaration {

    @JsonProperty("id")
    private String id;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private Object inputSchema;

    @JsonProperty("actions")
    private List<CanvasAction> actions;

    /**
     * Creates an empty canvas declaration.
     */
    public CanvasDeclaration() {
    }

    /**
     * Creates a canvas declaration with the required fields set.
     *
     * @param id
     *            the canvas identifier, unique within the declaring connection
     * @param displayName
     *            the human-readable name shown in host UI and canvas pickers
     * @param description
     *            a short, single-sentence description shown to the agent in canvas
     *            catalogs
     */
    public CanvasDeclaration(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the canvas identifier.
     *
     * @return the canvas id, unique within the declaring connection
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the canvas identifier, unique within the declaring connection.
     *
     * @param id
     *            the canvas id
     * @return this declaration for method chaining
     */
    public CanvasDeclaration setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name shown in host UI and canvas pickers
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human-readable name shown in host UI and canvas pickers.
     *
     * @param displayName
     *            the display name
     * @return this declaration for method chaining
     */
    public CanvasDeclaration setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Gets the short description shown to the agent.
     *
     * @return the single-sentence description shown in canvas catalogs
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the short, single-sentence description shown to the agent in canvas
     * catalogs.
     *
     * @param description
     *            the description
     * @return this declaration for method chaining
     */
    public CanvasDeclaration setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Gets the JSON Schema for the {@code input} payload accepted by
     * {@code canvas.open}.
     *
     * @return the input schema, or {@code null} if none
     */
    public Object getInputSchema() {
        return inputSchema;
    }

    /**
     * Sets the JSON Schema for the {@code input} payload accepted by
     * {@code canvas.open}.
     *
     * @param inputSchema
     *            the input schema as a JSON-serializable value (e.g. a {@code Map})
     * @return this declaration for method chaining
     */
    public CanvasDeclaration setInputSchema(Object inputSchema) {
        this.inputSchema = inputSchema;
        return this;
    }

    /**
     * Gets the agent-callable actions this canvas exposes.
     *
     * @return the actions, or {@code null} if none
     */
    public List<CanvasAction> getActions() {
        return actions;
    }

    /**
     * Sets the agent-callable actions this canvas exposes via
     * {@code invoke_canvas_action}.
     *
     * @param actions
     *            the actions
     * @return this declaration for method chaining
     */
    public CanvasDeclaration setActions(List<CanvasAction> actions) {
        this.actions = actions;
        return this;
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Context for an elicitation request received from the server or MCP tools.
 *
 * @since 1.0.0
 */
public class ElicitationContext {

    private String sessionId;
    private String message;
    private ElicitationSchema requestedSchema;
    private String mode;
    private String elicitationSource;
    private String url;

    /**
     * Gets the session ID that triggered the elicitation request. @return the
     * session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /** Sets the session ID. @param sessionId the session ID @return this */
    public ElicitationContext setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets the message describing what information is needed from the user.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /** Sets the message. @param message the message @return this */
    public ElicitationContext setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Gets the JSON Schema describing the form fields to present (form mode only).
     *
     * @return the schema, or {@code null}
     */
    public ElicitationSchema getRequestedSchema() {
        return requestedSchema;
    }

    /** Sets the schema. @param requestedSchema the schema @return this */
    public ElicitationContext setRequestedSchema(ElicitationSchema requestedSchema) {
        this.requestedSchema = requestedSchema;
        return this;
    }

    /**
     * Gets the elicitation mode: {@code "form"} for structured input, {@code "url"}
     * for browser redirect.
     *
     * @return the mode, or {@code null} (defaults to {@code "form"})
     */
    public String getMode() {
        return mode;
    }

    /** Sets the mode. @param mode the mode @return this */
    public ElicitationContext setMode(String mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Gets the source that initiated the request (e.g., MCP server name).
     *
     * @return the elicitation source, or {@code null}
     */
    public String getElicitationSource() {
        return elicitationSource;
    }

    /**
     * Sets the elicitation source. @param elicitationSource the source @return this
     */
    public ElicitationContext setElicitationSource(String elicitationSource) {
        this.elicitationSource = elicitationSource;
        return this;
    }

    /**
     * Gets the URL to open in the user's browser (url mode only).
     *
     * @return the URL, or {@code null}
     */
    public String getUrl() {
        return url;
    }

    /** Sets the URL. @param url the URL @return this */
    public ElicitationContext setUrl(String url) {
        this.url = url;
        return this;
    }
}

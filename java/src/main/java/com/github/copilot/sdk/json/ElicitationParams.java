/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Parameters for an elicitation request sent from the SDK to the host.
 *
 * @since 1.0.0
 */
public class ElicitationParams {

    private String message;
    private ElicitationSchema requestedSchema;

    /**
     * Gets the message describing what information is needed from the user.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message describing what information is needed from the user.
     *
     * @param message
     *            the message
     * @return this instance for method chaining
     */
    public ElicitationParams setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Gets the JSON Schema describing the form fields to present.
     *
     * @return the requested schema
     */
    public ElicitationSchema getRequestedSchema() {
        return requestedSchema;
    }

    /**
     * Sets the JSON Schema describing the form fields to present.
     *
     * @param requestedSchema
     *            the schema
     * @return this instance for method chaining
     */
    public ElicitationParams setRequestedSchema(ElicitationSchema requestedSchema) {
        this.requestedSchema = requestedSchema;
        return this;
    }
}

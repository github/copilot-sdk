/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON Schema describing the form fields to present for an elicitation dialog.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElicitationSchema {

    @JsonProperty("type")
    private String type = "object";

    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("required")
    private List<String> required;

    /**
     * Gets the schema type indicator (always {@code "object"}).
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the schema type indicator.
     *
     * @param type
     *            the type (typically {@code "object"})
     * @return this instance for method chaining
     */
    public ElicitationSchema setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the form field definitions, keyed by field name.
     *
     * @return the properties map
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Sets the form field definitions, keyed by field name.
     *
     * @param properties
     *            the properties map
     * @return this instance for method chaining
     */
    public ElicitationSchema setProperties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Gets the list of required field names.
     *
     * @return the required field names, or {@code null}
     */
    public List<String> getRequired() {
        return required;
    }

    /**
     * Sets the list of required field names.
     *
     * @param required
     *            the required field names
     * @return this instance for method chaining
     */
    public ElicitationSchema setRequired(List<String> required) {
        this.required = required;
        return this;
    }
}

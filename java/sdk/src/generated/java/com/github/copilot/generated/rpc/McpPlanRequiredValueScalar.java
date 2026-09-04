/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * One non-secret scalar value a transport choice needs before it can be applied.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpPlanRequiredValueScalar extends McpPlanRequiredValue {

    @JsonProperty("kind")
    private final String kind = "scalar";

    @Override
    public String getKind() { return kind; }

    /** Key the value is supplied under. Inert untrusted data. */
    @JsonProperty("key")
    private String key;

    /** Where the value is applied when the server is launched. */
    @JsonProperty("category")
    private McpPlanValueCategory category;

    /** Scalar type the value must conform to. */
    @JsonProperty("valueType")
    private McpPlanScalarValueType valueType;

    /** Whether the value must be present for the plan to be applicable. */
    @JsonProperty("required")
    private Boolean required;

    /** Default supplied by the card, when the value can be resolved without input. Presence is the authoritative indication that a default exists. Inert untrusted data. */
    @JsonProperty("defaultValue")
    private String defaultValue;

    /** Human-readable label from the card. Inert untrusted text. */
    @JsonProperty("title")
    private String title;

    /** Human-readable explanation from the card. Inert untrusted text. */
    @JsonProperty("description")
    private String description;

    /** Whether the value may be supplied more than once. */
    @JsonProperty("isRepeated")
    private Boolean isRepeated;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public McpPlanValueCategory getCategory() { return category; }
    public void setCategory(McpPlanValueCategory category) { this.category = category; }

    public McpPlanScalarValueType getValueType() { return valueType; }
    public void setValueType(McpPlanScalarValueType valueType) { this.valueType = valueType; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsRepeated() { return isRepeated; }
    public void setIsRepeated(Boolean isRepeated) { this.isRepeated = isRepeated; }
}

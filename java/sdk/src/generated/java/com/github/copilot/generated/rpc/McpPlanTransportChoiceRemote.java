/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * An eligible remote-endpoint transport choice. The endpoint is required and package identity cannot be represented.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpPlanTransportChoiceRemote extends McpPlanTransportChoice {

    @JsonProperty("installMethod")
    private final String installMethod = "remote";

    @Override
    public String getInstallMethod() { return installMethod; }

    /** Stable identifier for this choice within the plan, used to select it when the plan is applied. */
    @JsonProperty("choiceId")
    private String choiceId;

    /** Endpoint transport this remote choice would use. */
    @JsonProperty("transport")
    private McpPlanRemoteTransport transport;

    /** Endpoint URL. Inert untrusted data. */
    @JsonProperty("endpoint")
    private String endpoint;

    /** Typed values this choice requires, excluding secrets. */
    @JsonProperty("requiredValues")
    private List<McpPlanRequiredValue> requiredValues;

    /** Secrets this choice requires, referenced by placeholder only. */
    @JsonProperty("secretPlaceholders")
    private List<McpPlanSecretPlaceholder> secretPlaceholders;

    public String getChoiceId() { return choiceId; }
    public void setChoiceId(String choiceId) { this.choiceId = choiceId; }

    public McpPlanRemoteTransport getTransport() { return transport; }
    public void setTransport(McpPlanRemoteTransport transport) { this.transport = transport; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public List<McpPlanRequiredValue> getRequiredValues() { return requiredValues; }
    public void setRequiredValues(List<McpPlanRequiredValue> requiredValues) { this.requiredValues = requiredValues; }

    public List<McpPlanSecretPlaceholder> getSecretPlaceholders() { return secretPlaceholders; }
    public void setSecretPlaceholders(List<McpPlanSecretPlaceholder> secretPlaceholders) { this.secretPlaceholders = secretPlaceholders; }
}

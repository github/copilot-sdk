/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * An eligible remote-endpoint transport choice. The endpoint is required and package identity cannot be represented.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpPlanTransportChoiceRemote(
    /** Stable identifier for this choice within the plan, used to select it when the plan is applied. */
    @JsonProperty("choiceId") String choiceId,
    /** Endpoint transport this remote choice would use. */
    @JsonProperty("transport") McpPlanRemoteTransport transport,
    /** Discriminator: this choice connects to a remote endpoint */
    @JsonProperty("installMethod") String installMethod,
    /** Endpoint URL. Inert untrusted data. */
    @JsonProperty("endpoint") String endpoint,
    /** Typed values this choice requires, excluding secrets. */
    @JsonProperty("requiredValues") List<Object> requiredValues,
    /** Secrets this choice requires, referenced by placeholder only. */
    @JsonProperty("secretPlaceholders") List<McpPlanSecretPlaceholder> secretPlaceholders
) implements McpPlanTransportChoice {
    public McpPlanTransportChoiceRemote {
        installMethod = "remote";
    }

    public McpPlanTransportChoiceRemote(
        String choiceId,
        McpPlanRemoteTransport transport,
        String endpoint,
        List<Object> requiredValues,
        List<McpPlanSecretPlaceholder> secretPlaceholders
    ) {
        this(choiceId, transport, "remote", endpoint, requiredValues, secretPlaceholders);
    }
}

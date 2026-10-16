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
 * An eligible local-package transport choice. Package identity is required and a remote endpoint cannot be represented.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpPlanTransportChoicePackage(
    /** Stable identifier for this choice within the plan, used to select it when the plan is applied. */
    @JsonProperty("choiceId") String choiceId,
    /** Local process transport this package choice would use. */
    @JsonProperty("transport") McpPlanPackageTransport transport,
    /** Discriminator: this choice runs a local package */
    @JsonProperty("installMethod") String installMethod,
    /** Packaging ecosystem, for example `oci` or `npm`. */
    @JsonProperty("packageType") String packageType,
    /** Package identifier. Inert untrusted data. */
    @JsonProperty("packageIdentifier") String packageIdentifier,
    /** Typed values this choice requires, excluding secrets. */
    @JsonProperty("requiredValues") List<Object> requiredValues,
    /** Secrets this choice requires, referenced by placeholder only. */
    @JsonProperty("secretPlaceholders") List<McpPlanSecretPlaceholder> secretPlaceholders
) implements McpPlanTransportChoice {
    public McpPlanTransportChoicePackage {
        installMethod = "package";
    }

    public McpPlanTransportChoicePackage(
        String choiceId,
        McpPlanPackageTransport transport,
        String packageType,
        String packageIdentifier,
        List<Object> requiredValues,
        List<McpPlanSecretPlaceholder> secretPlaceholders
    ) {
        this(choiceId, transport, "package", packageType, packageIdentifier, requiredValues, secretPlaceholders);
    }
}

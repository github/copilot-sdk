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
 * An eligible local-package transport choice. Package identity is required and a remote endpoint cannot be represented.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpPlanTransportChoicePackage extends McpPlanTransportChoice {

    @JsonProperty("installMethod")
    private final String installMethod = "package";

    @Override
    public String getInstallMethod() { return installMethod; }

    /** Stable identifier for this choice within the plan, used to select it when the plan is applied. */
    @JsonProperty("choiceId")
    private String choiceId;

    /** Local process transport this package choice would use. */
    @JsonProperty("transport")
    private McpPlanPackageTransport transport;

    /** Packaging ecosystem, for example `oci` or `npm`. */
    @JsonProperty("packageType")
    private String packageType;

    /** Package identifier. Inert untrusted data. */
    @JsonProperty("packageIdentifier")
    private String packageIdentifier;

    /** Typed values this choice requires, excluding secrets. */
    @JsonProperty("requiredValues")
    private List<McpPlanRequiredValue> requiredValues;

    /** Secrets this choice requires, referenced by placeholder only. */
    @JsonProperty("secretPlaceholders")
    private List<McpPlanSecretPlaceholder> secretPlaceholders;

    public String getChoiceId() { return choiceId; }
    public void setChoiceId(String choiceId) { this.choiceId = choiceId; }

    public McpPlanPackageTransport getTransport() { return transport; }
    public void setTransport(McpPlanPackageTransport transport) { this.transport = transport; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    public String getPackageIdentifier() { return packageIdentifier; }
    public void setPackageIdentifier(String packageIdentifier) { this.packageIdentifier = packageIdentifier; }

    public List<McpPlanRequiredValue> getRequiredValues() { return requiredValues; }
    public void setRequiredValues(List<McpPlanRequiredValue> requiredValues) { this.requiredValues = requiredValues; }

    public List<McpPlanSecretPlaceholder> getSecretPlaceholders() { return secretPlaceholders; }
    public void setSecretPlaceholders(List<McpPlanSecretPlaceholder> secretPlaceholders) { this.secretPlaceholders = secretPlaceholders; }
}

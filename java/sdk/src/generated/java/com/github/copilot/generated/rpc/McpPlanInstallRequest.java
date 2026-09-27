/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class McpPlanInstallRequest {

    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract")
    private final CatalogClientContract contract;

    /** What to plan: either a candidate handle from a previous search, or a card supplied directly. */
    @JsonProperty("source")
    private final Object source;

    /** Configuration scope the plan targets. Defaults to user scope when omitted. */
    @JsonProperty("scope")
    private McpPlanScope scope;

    /** The same existing attached session that owns the original catalogue candidate. */
    @JsonProperty("policySessionId")
    private String policySessionId;

    /**
     * Creates a request with its required inputs.
     *
     * @param contract Protocol version and capabilities the caller requires.
     * @param source What to plan: either a candidate handle from a previous search, or a card supplied directly.
     */
    public McpPlanInstallRequest(CatalogClientContract contract, Object source) {
        this.contract = Objects.requireNonNull(contract, "contract");
        this.source = Objects.requireNonNull(source, "source");
    }

    /**
     * Returns the {@code contract} property.
     *
     * @return Protocol version and capabilities the caller requires.
     */
    public CatalogClientContract getContract() {
        return contract;
    }

    /**
     * Returns the {@code source} property.
     *
     * @return What to plan: either a candidate handle from a previous search, or a card supplied directly.
     */
    public Object getSource() {
        return source;
    }

    /**
     * Returns the {@code scope} property.
     *
     * @return Configuration scope the plan targets. Defaults to user scope when omitted.
     */
    public McpPlanScope getScope() {
        return scope;
    }

    /**
     * Returns the {@code policySessionId} property.
     *
     * @return The same existing attached session that owns the original catalogue candidate.
     */
    public String getPolicySessionId() {
        return policySessionId;
    }

    /**
     * Sets the {@code scope} property.
     *
     * @param value Configuration scope the plan targets. Defaults to user scope when omitted.
     * @return this request
     */
    public McpPlanInstallRequest setScope(McpPlanScope value) {
        this.scope = value;
        return this;
    }

    /**
     * Sets the {@code policySessionId} property.
     *
     * @param value The same existing attached session that owns the original catalogue candidate.
     * @return this request
     */
    public McpPlanInstallRequest setPolicySessionId(String value) {
        this.policySessionId = value;
        return this;
    }
}

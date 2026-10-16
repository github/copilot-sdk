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
 * Requests authoritative Connector-to-MCP reconciliation for the pinned account.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionConnectorsReconcileRequest {

    /** Opaque account selection ID. It must match the account already pinned to the session, if any. */
    @JsonProperty("accountId")
    private final String accountId;

    /** When true, refresh the catalog before reconciling. A disabled Connector API performs no service request. */
    @JsonProperty("refreshCatalog")
    private Boolean refreshCatalog;

    /** Optional Connector name to reinitialize. Requires the targetedReconcile capability. */
    @JsonProperty("forceConnectorName")
    private String forceConnectorName;

    /**
     * Creates a request with its required inputs.
     *
     * @param accountId Opaque account selection ID. It must match the account already pinned to the session, if any.
     */
    public SessionConnectorsReconcileRequest(String accountId) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
    }

    /**
     * Returns the {@code accountId} property.
     *
     * @return Opaque account selection ID. It must match the account already pinned to the session, if any.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Returns the {@code refreshCatalog} property.
     *
     * @return When true, refresh the catalog before reconciling. A disabled Connector API performs no service request.
     */
    public Boolean getRefreshCatalog() {
        return refreshCatalog;
    }

    /**
     * Returns the {@code forceConnectorName} property.
     *
     * @return Optional Connector name to reinitialize. Requires the targetedReconcile capability.
     */
    public String getForceConnectorName() {
        return forceConnectorName;
    }

    /**
     * Sets the {@code refreshCatalog} property.
     *
     * @param value When true, refresh the catalog before reconciling. A disabled Connector API performs no service request.
     * @return this request
     */
    public SessionConnectorsReconcileRequest setRefreshCatalog(Boolean value) {
        this.refreshCatalog = value;
        return this;
    }

    /**
     * Sets the {@code forceConnectorName} property.
     *
     * @param value Optional Connector name to reinitialize. Requires the targetedReconcile capability.
     * @return this request
     */
    public SessionConnectorsReconcileRequest setForceConnectorName(String value) {
        this.forceConnectorName = value;
        return this;
    }
}

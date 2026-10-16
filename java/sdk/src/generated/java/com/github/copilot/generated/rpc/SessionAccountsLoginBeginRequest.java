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
 * Begin an interactive login flow for a provider kind, optionally reusing a cached base identity or explicitly requesting provider account selection.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionAccountsLoginBeginRequest {

    /** The provider kind to sign in with. */
    @JsonProperty("kind")
    private final LoginProviderKind kind;

    /** Neutral identity-selection intent. Omitted is equivalent to default. */
    @JsonProperty("mode")
    private AuthLoginMode mode;

    /** Opaque cached base identity id returned by accounts.enumerate loginAccounts. Valid only with default mode; consumers must not parse it. */
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Creates a request with its required inputs.
     *
     * @param kind The provider kind to sign in with.
     */
    public SessionAccountsLoginBeginRequest(LoginProviderKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    /**
     * Returns the {@code kind} property.
     *
     * @return The provider kind to sign in with.
     */
    public LoginProviderKind getKind() {
        return kind;
    }

    /**
     * Returns the {@code mode} property.
     *
     * @return Neutral identity-selection intent. Omitted is equivalent to default.
     */
    public AuthLoginMode getMode() {
        return mode;
    }

    /**
     * Returns the {@code accountId} property.
     *
     * @return Opaque cached base identity id returned by accounts.enumerate loginAccounts. Valid only with default mode; consumers must not parse it.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the {@code mode} property.
     *
     * @param value Neutral identity-selection intent. Omitted is equivalent to default.
     * @return this request
     */
    public SessionAccountsLoginBeginRequest setMode(AuthLoginMode value) {
        this.mode = value;
        return this;
    }

    /**
     * Sets the {@code accountId} property.
     *
     * @param value Opaque cached base identity id returned by accounts.enumerate loginAccounts. Valid only with default mode; consumers must not parse it.
     * @return this request
     */
    public SessionAccountsLoginBeginRequest setAccountId(String value) {
        this.accountId = value;
        return this;
    }
}

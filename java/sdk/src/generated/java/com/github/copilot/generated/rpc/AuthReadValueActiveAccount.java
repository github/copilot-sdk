/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Variant {@code activeAccount} of {@link AuthReadValue}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthReadValueActiveAccount extends AuthReadValue {

    @JsonProperty("kind")
    private final String kind = "activeAccount";

    @Override
    public String getKind() { return kind; }

    /** The active account, or absent when not logged in. */
    @JsonProperty("account")
    private AccountStatus account;

    public AccountStatus getAccount() { return account; }
    public void setAccount(AccountStatus account) { this.account = account; }
}

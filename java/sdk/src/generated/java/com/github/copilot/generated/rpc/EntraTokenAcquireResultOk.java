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
 * Variant {@code ok} of {@link EntraTokenAcquireResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EntraTokenAcquireResultOk extends EntraTokenAcquireResult {

    @JsonProperty("status")
    private final String status = "ok";

    @Override
    public String getStatus() { return status; }

    /** Opaque access token. */
    @JsonProperty("accessToken")
    private String accessToken;

    /** Expiry as milliseconds since Unix epoch, when supplied by OneAuth. */
    @JsonProperty("expiresOnTimestamp")
    private Double expiresOnTimestamp;

    /** Opaque OneAuth account id, when supplied by the broker. */
    @JsonProperty("accountId")
    private String accountId;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public Double getExpiresOnTimestamp() { return expiresOnTimestamp; }
    public void setExpiresOnTimestamp(Double expiresOnTimestamp) { this.expiresOnTimestamp = expiresOnTimestamp; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
}

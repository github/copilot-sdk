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
 * An account the host has signed in to, identified by the server it lives on and the login it uses there. The same person can appear more than once when they use both github.com and an Enterprise server.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record LoggedInUser(
    /** Host the account belongs to, such as `github.com` or an Enterprise server. */
    @JsonProperty("host") String host,
    /** Account login on that host. */
    @JsonProperty("login") String login,
    /** Account kind, when the host recorded one. Consumers must tolerate new strings. */
    @JsonProperty("kind") String kind,
    /** Source account this account was derived from, when one was recorded. */
    @JsonProperty("derivedFrom") String derivedFrom
) {
}

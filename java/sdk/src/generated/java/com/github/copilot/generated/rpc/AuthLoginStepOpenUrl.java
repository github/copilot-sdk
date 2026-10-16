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
 * Variant {@code open-url} of {@link AuthLoginStep}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthLoginStepOpenUrl extends AuthLoginStep {

    @JsonProperty("kind")
    private final String kind = "open-url";

    @Override
    public String getKind() { return kind; }

    /** Authorize URL the consumer should open in a browser (consumer-driven browser-open). */
    @JsonProperty("url")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

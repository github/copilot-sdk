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
 * Candidate whose card is retrieved from a URL through the runtime's hardened fetch boundary.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogCandidateSourceUrl extends CatalogCandidateSource {

    @JsonProperty("kind")
    private final String kind = "url";

    @Override
    public String getKind() { return kind; }

    /** Card URL as advertised. Inert untrusted data: the runtime retrieves it only through its own hardened boundary, and it is never logged. */
    @JsonProperty("url")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

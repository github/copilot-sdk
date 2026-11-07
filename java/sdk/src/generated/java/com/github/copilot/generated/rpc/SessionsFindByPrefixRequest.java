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
 * UUID prefix to resolve to a unique session ID.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionsFindByPrefixRequest {

    /** UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions. */
    @JsonProperty("prefix")
    private final String prefix;

    /** Configuration directory containing the session state. Omitted to use the server's configured home. */
    @JsonProperty("configDir")
    private String configDir;

    /**
     * Creates a request with its required inputs.
     *
     * @param prefix UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.
     */
    public SessionsFindByPrefixRequest(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    /**
     * Returns the {@code prefix} property.
     *
     * @return UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the {@code configDir} property.
     *
     * @return Configuration directory containing the session state. Omitted to use the server's configured home.
     */
    public String getConfigDir() {
        return configDir;
    }

    /**
     * Sets the {@code configDir} property.
     *
     * @param value Configuration directory containing the session state. Omitted to use the server's configured home.
     * @return this request
     */
    public SessionsFindByPrefixRequest setConfigDir(String value) {
        this.configDir = value;
        return this;
    }
}

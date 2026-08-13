/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Options for creating a remote session in the cloud.
 *
 * @since 1.5.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudSessionOptions {

    @JsonProperty("repository")
    private CloudSessionRepository repository;

    /**
     * Gets the optional GitHub repository metadata to associate with the cloud
     * session.
     *
     * @return the repository metadata, or {@code null} if not set
     */
    public CloudSessionRepository getRepository() {
        return repository;
    }

    /**
     * Sets the optional GitHub repository metadata to associate with the cloud
     * session.
     *
     * @param repository
     *            the repository metadata
     * @return this instance for method chaining
     */
    public CloudSessionOptions setRepository(CloudSessionRepository repository) {
        this.repository = repository;
        return this;
    }
}

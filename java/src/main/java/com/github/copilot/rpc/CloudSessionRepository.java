/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub repository metadata to associate with a cloud session.
 *
 * @since 1.5.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudSessionRepository {

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("name")
    private String name;

    @JsonProperty("branch")
    private String branch;

    /**
     * Gets the repository owner.
     *
     * @return the repository owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the repository owner.
     *
     * @param owner
     *            the repository owner
     * @return this instance for method chaining
     */
    public CloudSessionRepository setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Gets the repository name.
     *
     * @return the repository name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the repository name.
     *
     * @param name
     *            the repository name
     * @return this instance for method chaining
     */
    public CloudSessionRepository setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the optional branch name.
     *
     * @return the branch name, or {@code null} if not set
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Sets the optional branch name.
     *
     * @param branch
     *            the branch name
     * @return this instance for method chaining
     */
    public CloudSessionRepository setBranch(String branch) {
        this.branch = branch;
        return this;
    }
}

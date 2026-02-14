/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Working directory context for a session.
 * <p>
 * Contains information about the working directory where the session was
 * created, including git repository information if applicable.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionContext {

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("gitRoot")
    private String gitRoot;

    @JsonProperty("repository")
    private String repository;

    @JsonProperty("branch")
    private String branch;

    /**
     * Gets the working directory where the session was created.
     *
     * @return the current working directory path
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the working directory.
     *
     * @param cwd
     *            the current working directory path
     * @return this instance for method chaining
     */
    public SessionContext setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the git repository root directory.
     *
     * @return the git root path, or {@code null} if not in a git repository
     */
    public String getGitRoot() {
        return gitRoot;
    }

    /**
     * Sets the git repository root directory.
     *
     * @param gitRoot
     *            the git root path
     * @return this instance for method chaining
     */
    public SessionContext setGitRoot(String gitRoot) {
        this.gitRoot = gitRoot;
        return this;
    }

    /**
     * Gets the GitHub repository in "owner/repo" format.
     *
     * @return the repository identifier, or {@code null} if not available
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Sets the GitHub repository.
     *
     * @param repository
     *            the repository in "owner/repo" format
     * @return this instance for method chaining
     */
    public SessionContext setRepository(String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Gets the current git branch.
     *
     * @return the branch name, or {@code null} if not available
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Sets the git branch.
     *
     * @param branch
     *            the branch name
     * @return this instance for method chaining
     */
    public SessionContext setBranch(String branch) {
        this.branch = branch;
        return this;
    }
}

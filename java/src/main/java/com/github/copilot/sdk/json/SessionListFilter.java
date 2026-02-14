/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Filter options for listing sessions.
 * <p>
 * Allows filtering sessions by working directory context fields such as cwd,
 * git root, repository, or branch.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Filter sessions by repository
 * var filter = new SessionListFilter().setRepository("owner/repo");
 * var sessions = client.listSessions(filter).get();
 *
 * // Filter by working directory
 * var filter = new SessionListFilter().setCwd("/path/to/project");
 * var sessions = client.listSessions(filter).get();
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotClient#listSessions(SessionListFilter)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionListFilter {

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("gitRoot")
    private String gitRoot;

    @JsonProperty("repository")
    private String repository;

    @JsonProperty("branch")
    private String branch;

    /**
     * Gets the current working directory filter.
     *
     * @return the cwd filter, or {@code null} if not set
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the filter for exact cwd match.
     *
     * @param cwd
     *            the current working directory to filter by
     * @return this filter for method chaining
     */
    public SessionListFilter setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the git root filter.
     *
     * @return the git root filter, or {@code null} if not set
     */
    public String getGitRoot() {
        return gitRoot;
    }

    /**
     * Sets the filter for git root directory.
     *
     * @param gitRoot
     *            the git root path to filter by
     * @return this filter for method chaining
     */
    public SessionListFilter setGitRoot(String gitRoot) {
        this.gitRoot = gitRoot;
        return this;
    }

    /**
     * Gets the repository filter.
     *
     * @return the repository filter, or {@code null} if not set
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Sets the filter for repository (in "owner/repo" format).
     *
     * @param repository
     *            the repository identifier to filter by
     * @return this filter for method chaining
     */
    public SessionListFilter setRepository(String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Gets the branch filter.
     *
     * @return the branch filter, or {@code null} if not set
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Sets the filter for git branch.
     *
     * @param branch
     *            the branch name to filter by
     * @return this filter for method chaining
     */
    public SessionListFilter setBranch(String branch) {
        this.branch = branch;
        return this;
    }
}

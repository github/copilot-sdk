/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Filter options for listing sessions.
 * <p>
 * Extends {@link SessionContext} to provide filtering capabilities with fluent
 * setter methods that return the filter instance for method chaining.
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
public class SessionListFilter extends SessionContext {

    /**
     * Sets the filter for exact cwd match.
     *
     * @param cwd
     *            the current working directory to filter by
     * @return this filter for method chaining
     */
    @Override
    public SessionListFilter setCwd(String cwd) {
        super.setCwd(cwd);
        return this;
    }

    /**
     * Sets the filter for git root directory.
     *
     * @param gitRoot
     *            the git root path to filter by
     * @return this filter for method chaining
     */
    @Override
    public SessionListFilter setGitRoot(String gitRoot) {
        super.setGitRoot(gitRoot);
        return this;
    }

    /**
     * Sets the filter for repository (in "owner/repo" format).
     *
     * @param repository
     *            the repository identifier to filter by
     * @return this filter for method chaining
     */
    @Override
    public SessionListFilter setRepository(String repository) {
        super.setRepository(repository);
        return this;
    }

    /**
     * Sets the filter for git branch.
     *
     * @param branch
     *            the branch name to filter by
     * @return this filter for method chaining
     */
    @Override
    public SessionListFilter setBranch(String branch) {
        super.setBranch(branch);
        return this;
    }
}

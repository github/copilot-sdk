/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Connects to an already-running runtime at the configured URL. Construct with
 * {@link RuntimeConnection#forUri(String)}.
 *
 * @since 1.0.0
 */
public final class UriRuntimeConnection extends RuntimeConnection {

    private final String url;
    private String connectionToken;

    UriRuntimeConnection(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("UriRuntimeConnection url must be a non-empty string");
        }
        this.url = url;
    }

    /**
     * Returns the URL of the runtime to connect to.
     *
     * @return the URL; accepts {@code "port"}, {@code "host:port"}, or a full URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the shared secret used to authenticate the connection.
     *
     * @return the token, or {@code null} if the runtime does not require one
     */
    public String getConnectionToken() {
        return connectionToken;
    }

    /**
     * Sets the shared secret used to authenticate the connection.
     *
     * @param connectionToken
     *            the token, or {@code null} if the runtime does not require one
     * @return this instance for method chaining
     */
    public UriRuntimeConnection setConnectionToken(String connectionToken) {
        this.connectionToken = connectionToken;
        return this;
    }
}

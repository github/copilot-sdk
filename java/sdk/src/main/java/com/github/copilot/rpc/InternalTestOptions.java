/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.HashMap;
import java.util.Map;

import com.github.copilot.CopilotExperimental;

/**
 * Unsupported SDK test infrastructure configuration. Supplied values override
 * owned-runtime startup configuration without changing the host environment.
 * Null preserves the inherited value; an empty string clears an override.
 *
 * @hidden
 */
@CopilotExperimental
public final class InternalTestOptions {
    private String copilotApiUrl;
    private String gitHubApiUrl;
    private String proxyUrl;
    private String noProxy;
    private String caCertificatePath;

    /** @return the Copilot API replay endpoint */
    public String getCopilotApiUrl() {
        return copilotApiUrl;
    }

    /**
     * @param value
     *            Copilot API replay endpoint
     * @return this configuration
     */
    public InternalTestOptions setCopilotApiUrl(String value) {
        copilotApiUrl = value;
        return this;
    }

    /** @return the GitHub API replay endpoint, including startup authentication */
    public String getGitHubApiUrl() {
        return gitHubApiUrl;
    }

    /**
     * @param value
     *            GitHub API replay endpoint
     * @return this configuration
     */
    public InternalTestOptions setGitHubApiUrl(String value) {
        gitHubApiUrl = value;
        return this;
    }

    /** @return the HTTP/HTTPS interception proxy */
    public String getProxyUrl() {
        return proxyUrl;
    }

    /**
     * @param value
     *            HTTP/HTTPS interception proxy
     * @return this configuration
     */
    public InternalTestOptions setProxyUrl(String value) {
        proxyUrl = value;
        return this;
    }

    /** @return comma-separated proxy bypass destinations */
    public String getNoProxy() {
        return noProxy;
    }

    /**
     * @param value
     *            comma-separated proxy bypass destinations
     * @return this configuration
     */
    public InternalTestOptions setNoProxy(String value) {
        noProxy = value;
        return this;
    }

    /** @return the PEM CA-bundle path */
    public String getCaCertificatePath() {
        return caCertificatePath;
    }

    /**
     * @param value
     *            PEM CA-bundle path trusted by the runtime and its subprocesses
     * @return this configuration
     */
    public InternalTestOptions setCaCertificatePath(String value) {
        caCertificatePath = value;
        return this;
    }

    /**
     * Lowers the typed test configuration to the runtime startup representation.
     *
     * @return supplied overrides
     * @hidden
     */
    public Map<String, String> toEnvironment() {
        Map<String, String> result = new HashMap<>();
        for (String[] entry : new String[][]{{"COPILOT_API_URL", copilotApiUrl},
                {"COPILOT_DEBUG_GITHUB_API_URL", gitHubApiUrl}, {"HTTP_PROXY", proxyUrl}, {"HTTPS_PROXY", proxyUrl},
                {"http_proxy", proxyUrl}, {"https_proxy", proxyUrl}, {"NO_PROXY", noProxy}, {"no_proxy", noProxy},
                {"NODE_EXTRA_CA_CERTS", caCertificatePath}, {"SSL_CERT_FILE", caCertificatePath},
                {"REQUESTS_CA_BUNDLE", caCertificatePath}, {"CURL_CA_BUNDLE", caCertificatePath},
                {"GIT_SSL_CAINFO", caCertificatePath},}) {
            if (entry[1] != null) {
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }
}

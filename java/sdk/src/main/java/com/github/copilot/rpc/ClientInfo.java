/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identity of the integrating application, declared on the {@code server.connect}
 * handshake.
 * <p>
 * Declaring it lets the telemetry the runtime emits on the connection be
 * attributed to a single, consistent surface (the application and its Copilot
 * integration) instead of the runtime's own build. All fields are optional; an
 * empty field is omitted from the handshake.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var options = new CopilotClientOptions().setClientInfo(new ClientInfo()
 * 		.setApplicationName("acme-developer-portal").setApplicationVersion("2.4.0")
 * 		.setIntegrationName("copilot-assistant").setIntegrationVersion("1.5.0"));
 * }</pre>
 *
 * @see CopilotClientOptions#setClientInfo(ClientInfo)
 * @since 1.6.0
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ClientInfo {

    private String applicationName;

    private String applicationVersion;

    private String integrationName;

    private String integrationVersion;

    /**
     * Gets the name of the application using the SDK.
     *
     * @return the application name, or {@code null}
     */
    @JsonProperty("editorName")
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the name of the application using the SDK.
     *
     * @param applicationName
     *            the application name
     * @return this client info for method chaining
     */
    @JsonProperty("editorName")
    public ClientInfo setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * Gets the version of the application using the SDK.
     *
     * @return the application version, or {@code null}
     */
    @JsonProperty("editorVersion")
    public String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * Sets the version of the application using the SDK.
     *
     * @param applicationVersion
     *            the application version
     * @return this client info for method chaining
     */
    @JsonProperty("editorVersion")
    public ClientInfo setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
        return this;
    }

    /**
     * Gets the name of the Copilot integration within the application.
     *
     * @return the integration name, or {@code null}
     */
    @JsonProperty("extensionName")
    public String getIntegrationName() {
        return integrationName;
    }

    /**
     * Sets the name of the Copilot integration within the application.
     *
     * @param integrationName
     *            the integration name
     * @return this client info for method chaining
     */
    @JsonProperty("extensionName")
    public ClientInfo setIntegrationName(String integrationName) {
        this.integrationName = integrationName;
        return this;
    }

    /**
     * Gets the version of the Copilot integration within the application.
     *
     * @return the integration version, or {@code null}
     */
    @JsonProperty("extensionVersion")
    public String getIntegrationVersion() {
        return integrationVersion;
    }

    /**
     * Sets the version of the Copilot integration within the application.
     *
     * @param integrationVersion
     *            the integration version
     * @return this client info for method chaining
     */
    @JsonProperty("extensionVersion")
    public ClientInfo setIntegrationVersion(String integrationVersion) {
        this.integrationVersion = integrationVersion;
        return this;
    }

    /**
     * Returns whether no field carries a non-empty value, in which case the SDK
     * omits {@code clientInfo} from the handshake so the runtime keeps its default
     * attribution.
     *
     * @return {@code true} when every field is {@code null} or empty
     */
    @JsonIgnore
    public boolean isEmpty() {
        return isBlank(applicationName) && isBlank(applicationVersion) && isBlank(integrationName)
                && isBlank(integrationVersion);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}

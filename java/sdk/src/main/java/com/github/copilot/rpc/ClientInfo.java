/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identity of the integrating host, declared on the {@code server.connect}
 * handshake.
 * <p>
 * Declaring it lets the telemetry the runtime emits on the connection be
 * attributed to a single, consistent surface (the host editor and its Copilot
 * extension) instead of the runtime's own build. All fields are optional; an
 * unset field is omitted from the handshake.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var options = new CopilotClientOptions()
 * 		.setClientInfo(new ClientInfo().setEditorName("vscode").setEditorVersion("1.124.2")
 * 				.setExtensionName("copilot-chat").setExtensionVersion("0.54.0"));
 * }</pre>
 *
 * @see CopilotClientOptions#setClientInfo(ClientInfo)
 * @since 1.6.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientInfo {

    @JsonProperty("editorName")
    private String editorName;

    @JsonProperty("editorVersion")
    private String editorVersion;

    @JsonProperty("extensionName")
    private String extensionName;

    @JsonProperty("extensionVersion")
    private String extensionVersion;

    /**
     * Gets the name of the host editor.
     *
     * @return the editor name (e.g., {@code "vscode"}), or {@code null}
     */
    public String getEditorName() {
        return editorName;
    }

    /**
     * Sets the name of the host editor.
     *
     * @param editorName
     *            the editor name (e.g., {@code "vscode"})
     * @return this client info for method chaining
     */
    public ClientInfo setEditorName(String editorName) {
        this.editorName = editorName;
        return this;
    }

    /**
     * Gets the version of the host editor.
     *
     * @return the editor version (e.g., {@code "1.124.2"}), or {@code null}
     */
    public String getEditorVersion() {
        return editorVersion;
    }

    /**
     * Sets the version of the host editor.
     *
     * @param editorVersion
     *            the editor version (e.g., {@code "1.124.2"})
     * @return this client info for method chaining
     */
    public ClientInfo setEditorVersion(String editorVersion) {
        this.editorVersion = editorVersion;
        return this;
    }

    /**
     * Gets the name of the Copilot extension within the host.
     *
     * @return the extension name (e.g., {@code "copilot-chat"}), or {@code null}
     */
    public String getExtensionName() {
        return extensionName;
    }

    /**
     * Sets the name of the Copilot extension within the host.
     *
     * @param extensionName
     *            the extension name (e.g., {@code "copilot-chat"})
     * @return this client info for method chaining
     */
    public ClientInfo setExtensionName(String extensionName) {
        this.extensionName = extensionName;
        return this;
    }

    /**
     * Gets the version of the Copilot extension within the host.
     *
     * @return the extension version (e.g., {@code "0.54.0"}), or {@code null}
     */
    public String getExtensionVersion() {
        return extensionVersion;
    }

    /**
     * Sets the version of the Copilot extension within the host.
     *
     * @param extensionVersion
     *            the extension version (e.g., {@code "0.54.0"})
     * @return this client info for method chaining
     */
    public ClientInfo setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
        return this;
    }

    /**
     * Returns whether no field is set, in which case the SDK omits
     * {@code clientInfo} from the handshake so the runtime keeps its default
     * attribution.
     *
     * @return {@code true} when all fields are {@code null}
     */
    public boolean isEmpty() {
        return editorName == null && editorVersion == null && extensionName == null && extensionVersion == null;
    }
}

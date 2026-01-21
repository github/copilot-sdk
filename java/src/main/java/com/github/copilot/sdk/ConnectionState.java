/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

/**
 * Represents the connection state of a {@link CopilotClient}.
 * <p>
 * The connection state indicates the current status of the client's connection
 * to the Copilot CLI server.
 *
 * @see CopilotClient#getState()
 */
public enum ConnectionState {
    /**
     * The client is not connected to the server.
     */
    DISCONNECTED,

    /**
     * The client is in the process of connecting to the server.
     */
    CONNECTING,

    /**
     * The client is connected and ready to accept requests.
     */
    CONNECTED,

    /**
     * The client encountered an error during connection or operation.
     */
    ERROR
}

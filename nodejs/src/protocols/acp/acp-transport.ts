/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * NDJSON (Newline-Delimited JSON) transport for ACP protocol.
 * @module protocols/acp/acp-transport
 */

import type { Readable, Writable } from "node:stream";
import type { AcpMessage, AcpNotification, AcpRequest, AcpResponse, AcpError } from "./acp-types.js";

/**
 * Pending request tracker
 */
interface PendingRequest {
    resolve: (result: unknown) => void;
    reject: (error: Error) => void;
}

/**
 * NDJSON transport for ACP protocol.
 * Handles reading and writing JSON-RPC 2.0 messages over newline-delimited streams.
 */
export class AcpTransport {
    private buffer = "";
    private pendingRequests: Map<string | number, PendingRequest> = new Map();
    private notificationHandlers: Map<string, Set<(params: unknown) => void>> = new Map();
    private requestHandlers: Map<string, (id: string | number, params: unknown) => Promise<void>> =
        new Map();
    private messageHandlers: Set<(message: AcpMessage) => void> = new Set();
    private closeHandlers: Set<() => void> = new Set();
    private errorHandlers: Set<(error: Error) => void> = new Set();
    private isListening = false;
    private disposed = false;

    constructor(
        private readonly inputStream: Readable,
        private readonly outputStream: Writable
    ) {}

    /**
     * Starts listening for incoming messages.
     */
    listen(): void {
        if (this.isListening || this.disposed) {
            return;
        }
        this.isListening = true;

        this.inputStream.on("data", (chunk: Buffer) => {
            if (this.disposed) return;
            this.handleData(chunk.toString());
        });

        this.inputStream.on("end", () => {
            if (this.disposed) return;
            this.handleClose();
        });

        this.inputStream.on("error", (error: Error) => {
            if (this.disposed) return;
            this.emitError(error);
        });
    }

    /**
     * Sends a raw message.
     */
    send(message: AcpMessage): void {
        const json = JSON.stringify(message) + "\n";
        this.outputStream.write(json);
    }

    /**
     * Sends a request and returns a promise for the response.
     */
    sendRequest<T = unknown>(id: string | number, method: string, params?: unknown): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            const request: AcpRequest = {
                jsonrpc: "2.0",
                id,
                method,
                params,
            };

            this.pendingRequests.set(id, {
                resolve: resolve as (result: unknown) => void,
                reject,
            });

            this.send(request);
        });
    }

    /**
     * Sends a notification (no response expected).
     */
    sendNotification(method: string, params?: unknown): void {
        const notification: AcpNotification = {
            jsonrpc: "2.0",
            method,
            params,
        };
        this.send(notification);
    }

    /**
     * Registers a handler for all incoming messages.
     */
    onMessage(handler: (message: AcpMessage) => void): void {
        this.messageHandlers.add(handler);
    }

    /**
     * Registers a handler for a specific notification method.
     */
    onNotification(method: string, handler: (params: unknown) => void): void {
        if (!this.notificationHandlers.has(method)) {
            this.notificationHandlers.set(method, new Set());
        }
        this.notificationHandlers.get(method)!.add(handler);
    }

    /**
     * Registers a handler for incoming requests (server-to-client).
     */
    onRequest(method: string, handler: (id: string | number, params: unknown) => Promise<void>): void {
        this.requestHandlers.set(method, handler);
    }

    /**
     * Sends a response to an incoming request.
     */
    sendResponse(id: string | number, result: unknown): void {
        const response: AcpResponse = {
            jsonrpc: "2.0",
            id,
            result,
        };
        this.send(response);
    }

    /**
     * Registers a handler for close events.
     */
    onClose(handler: () => void): void {
        this.closeHandlers.add(handler);
    }

    /**
     * Registers a handler for error events.
     */
    onError(handler: (error: Error) => void): void {
        this.errorHandlers.add(handler);
    }

    /**
     * Disposes of the transport and releases resources.
     */
    dispose(): void {
        this.disposed = true;
        this.isListening = false;

        // Reject all pending requests
        for (const pending of this.pendingRequests.values()) {
            pending.reject(new Error("Transport disposed"));
        }
        this.pendingRequests.clear();

        // Clear handlers
        this.messageHandlers.clear();
        this.notificationHandlers.clear();
        this.requestHandlers.clear();
        this.closeHandlers.clear();
        this.errorHandlers.clear();
    }

    private handleData(data: string): void {
        this.buffer += data;

        // Process complete lines
        let newlineIndex: number;
        while ((newlineIndex = this.buffer.indexOf("\n")) !== -1) {
            const line = this.buffer.slice(0, newlineIndex);
            this.buffer = this.buffer.slice(newlineIndex + 1);

            // Skip empty lines
            if (line.trim() === "") {
                continue;
            }

            this.processLine(line);
        }
    }

    private processLine(line: string): void {
        let message: AcpMessage;

        try {
            message = JSON.parse(line) as AcpMessage;
        } catch (_error) {
            this.emitError(new Error(`Failed to parse JSON: ${line}`));
            return;
        }

        // Emit to general message handlers
        for (const handler of this.messageHandlers) {
            try {
                handler(message);
            } catch {
                // Ignore handler errors
            }
        }

        // Check if it's a response (has id but no method)
        if ("id" in message && !("method" in message)) {
            this.handleResponse(message as AcpResponse);
            return;
        }

        // Check if it's a notification (has method but no id)
        if ("method" in message && !("id" in message)) {
            this.handleNotification(message as AcpNotification);
            return;
        }

        // Request with both id and method - incoming request from server
        if ("id" in message && "method" in message) {
            this.handleIncomingRequest(message as AcpRequest);
            return;
        }
    }

    private handleIncomingRequest(request: AcpRequest): void {
        const handler = this.requestHandlers.get(request.method);
        if (!handler) {
            // No handler registered, send error response
            const errorResponse: AcpResponse = {
                jsonrpc: "2.0",
                id: request.id,
                error: {
                    code: -32601,
                    message: `Method not found: ${request.method}`,
                },
            };
            this.send(errorResponse);
            return;
        }

        // Call the handler - it's responsible for sending the response
        handler(request.id, request.params).catch((error) => {
            // If handler fails, send error response
            const errorResponse: AcpResponse = {
                jsonrpc: "2.0",
                id: request.id,
                error: {
                    code: -32603,
                    message: error instanceof Error ? error.message : "Internal error",
                },
            };
            this.send(errorResponse);
        });
    }

    private handleResponse(response: AcpResponse): void {
        const pending = this.pendingRequests.get(response.id);
        if (!pending) {
            return;
        }

        this.pendingRequests.delete(response.id);

        if (response.error) {
            const error = this.createError(response.error);
            pending.reject(error);
        } else {
            pending.resolve(response.result);
        }
    }

    private handleNotification(notification: AcpNotification): void {
        const handlers = this.notificationHandlers.get(notification.method);
        if (!handlers) {
            return;
        }

        for (const handler of handlers) {
            try {
                handler(notification.params);
            } catch {
                // Ignore handler errors
            }
        }
    }

    private handleClose(): void {
        // Reject all pending requests
        for (const pending of this.pendingRequests.values()) {
            pending.reject(new Error("Connection closed"));
        }
        this.pendingRequests.clear();

        // Emit close event
        for (const handler of this.closeHandlers) {
            try {
                handler();
            } catch {
                // Ignore handler errors
            }
        }
    }

    private emitError(error: Error): void {
        for (const handler of this.errorHandlers) {
            try {
                handler(error);
            } catch {
                // Ignore handler errors
            }
        }
    }

    private createError(acpError: AcpError): Error {
        const error = new Error(acpError.message);
        (error as Error & { code?: number }).code = acpError.code;
        return error;
    }
}

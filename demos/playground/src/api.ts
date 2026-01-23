/**
 * API client for communicating with the playground server
 *
 * All SDK operations go through these endpoints. The server manages
 * the actual SDK client and CLI process.
 *
 * ⚠️ NOTE: This is a toy implementation for local development only.
 * It does not include important production concerns like:
 * - Authentication/authorization
 * - Request validation
 * - Rate limiting
 * - CSRF protection
 * - Error handling for network failures
 */

import type {
    CreateSessionOptions,
    CreateSessionResponse,
    EventsResponse,
    MessagesResponse,
    StatusResponse,
} from "./types";

const API_BASE = "/api";

/**
 * Check if the SDK is connected and get active sessions
 */
export async function getStatus(): Promise<StatusResponse> {
    const response = await fetch(`${API_BASE}/status`);
    if (!response.ok) {
        throw new Error("Failed to get status");
    }
    return response.json();
}

/**
 * Create a new session with the specified options
 */
export async function createSession(options: CreateSessionOptions): Promise<CreateSessionResponse> {
    const response = await fetch(`${API_BASE}/sessions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(options),
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || "Failed to create session");
    }

    return response.json();
}

/**
 * Destroy a session
 */
export async function destroySession(sessionId: string): Promise<void> {
    const response = await fetch(`${API_BASE}/sessions/${sessionId}`, {
        method: "DELETE",
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || "Failed to destroy session");
    }
}

/**
 * Send a message to a session
 */
export async function sendMessage(
    sessionId: string,
    prompt: string
): Promise<{ messageId: string }> {
    const response = await fetch(`${API_BASE}/sessions/${sessionId}/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt }),
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || "Failed to send message");
    }

    return response.json();
}

/**
 * Get events for a session (used for polling)
 */
export async function getEvents(sessionId: string): Promise<EventsResponse> {
    const response = await fetch(`${API_BASE}/sessions/${sessionId}/events`);

    if (!response.ok) {
        if (response.status === 404) {
            throw new Error("Session not found");
        }
        throw new Error("Failed to get events");
    }

    return response.json();
}

/**
 * Get message history for a session
 */
export async function getMessages(sessionId: string): Promise<MessagesResponse> {
    const response = await fetch(`${API_BASE}/sessions/${sessionId}/messages`);

    if (!response.ok) {
        if (response.status === 404) {
            throw new Error("Session not found");
        }
        throw new Error("Failed to get messages");
    }

    return response.json();
}

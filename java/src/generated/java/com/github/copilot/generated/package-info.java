/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

/**
 * Auto-generated session event types for the GitHub Copilot SDK.
 *
 * <p>
 * This package contains Java classes generated from the Copilot CLI's
 * {@code session-events.schema.json}. Each event type corresponds to a
 * notification emitted during a {@link com.github.copilot.CopilotSession}
 * interaction.
 *
 * <h2>Key Classes</h2>
 * <ul>
 * <li>{@link com.github.copilot.generated.SessionEvent} - Abstract sealed base
 * class for all session events. Deserialized polymorphically via the
 * {@code type} discriminator.</li>
 * <li>{@link com.github.copilot.generated.UnknownSessionEvent} - Fallback for
 * event types not yet known to this SDK version, preserving forward
 * compatibility.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * session.on(AssistantMessageEvent.class, msg -> {
 *     System.out.println(msg.getData().content());
 * });
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 * <li>{@link com.github.copilot} - Core SDK classes</li>
 * <li>{@link com.github.copilot.generated.rpc} - Auto-generated RPC
 * parameter and result types</li>
 * </ul>
 *
 * @see com.github.copilot.CopilotSession
 * @see com.github.copilot.generated.SessionEvent
 */
package com.github.copilot.generated;

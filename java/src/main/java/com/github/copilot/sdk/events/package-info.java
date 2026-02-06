/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DTOs for JSON deserialization - low risk")
/**
 * Event types emitted during Copilot session processing.
 *
 * <p>
 * This package contains all event classes that can be emitted by a
 * {@link com.github.copilot.sdk.CopilotSession} during message processing.
 * Events provide real-time information about the session state, assistant
 * responses, tool executions, and other activities.
 *
 * <h2>Event Hierarchy</h2>
 * <p>
 * All events extend {@link com.github.copilot.sdk.events.AbstractSessionEvent},
 * which provides common properties like event type and timestamp.
 *
 * <h2>Key Event Types</h2>
 *
 * <h3>Message Events</h3>
 * <ul>
 * <li>{@link com.github.copilot.sdk.events.UserMessageEvent} - User message was
 * sent</li>
 * <li>{@link com.github.copilot.sdk.events.AssistantMessageEvent} - Complete
 * assistant response</li>
 * <li>{@link com.github.copilot.sdk.events.AssistantMessageDeltaEvent} -
 * Streaming response chunk</li>
 * <li>{@link com.github.copilot.sdk.events.SystemMessageEvent} - System message
 * added</li>
 * </ul>
 *
 * <h3>Session Lifecycle Events</h3>
 * <ul>
 * <li>{@link com.github.copilot.sdk.events.SessionStartEvent} - Session has
 * started</li>
 * <li>{@link com.github.copilot.sdk.events.SessionIdleEvent} - Session is idle
 * (processing complete)</li>
 * <li>{@link com.github.copilot.sdk.events.SessionErrorEvent} - An error
 * occurred</li>
 * <li>{@link com.github.copilot.sdk.events.SessionResumeEvent} - Session was
 * resumed</li>
 * </ul>
 *
 * <h3>Tool Execution Events</h3>
 * <ul>
 * <li>{@link com.github.copilot.sdk.events.ToolExecutionStartEvent} - Tool
 * execution started</li>
 * <li>{@link com.github.copilot.sdk.events.ToolExecutionCompleteEvent} - Tool
 * execution completed</li>
 * <li>{@link com.github.copilot.sdk.events.ToolExecutionProgressEvent} - Tool
 * execution progress update</li>
 * </ul>
 *
 * <h3>Subagent Events</h3>
 * <ul>
 * <li>{@link com.github.copilot.sdk.events.SubagentSelectedEvent} - Subagent
 * was selected</li>
 * <li>{@link com.github.copilot.sdk.events.SubagentStartedEvent} - Subagent
 * execution started</li>
 * <li>{@link com.github.copilot.sdk.events.SubagentCompletedEvent} - Subagent
 * execution completed</li>
 * <li>{@link com.github.copilot.sdk.events.SubagentFailedEvent} - Subagent
 * execution failed</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * session.on(evt -> {
 * 	if (evt instanceof AssistantMessageDeltaEvent delta) {
 * 		// Streaming response - print incrementally
 * 		System.out.print(delta.getData().getDeltaContent());
 * 	} else if (evt instanceof AssistantMessageEvent msg) {
 * 		// Complete response
 * 		System.out.println("\nFinal: " + msg.getData().getContent());
 * 	} else if (evt instanceof ToolExecutionStartEvent tool) {
 * 		System.out.println("Executing tool: " + tool.getData().getName());
 * 	} else if (evt instanceof SessionIdleEvent) {
 * 		System.out.println("Session is idle");
 * 	} else if (evt instanceof SessionErrorEvent err) {
 * 		System.err.println("Error: " + err.getData().getMessage());
 * 	}
 * });
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotSession#on(java.util.function.Consumer)
 * @see com.github.copilot.sdk.events.AbstractSessionEvent
 */
package com.github.copilot.sdk.events;

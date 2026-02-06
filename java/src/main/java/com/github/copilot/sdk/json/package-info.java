/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DTOs for JSON deserialization - low risk")
/**
 * Configuration classes and data transfer objects for the Copilot SDK.
 *
 * <p>
 * This package contains all the configuration, request, response, and data
 * transfer objects used throughout the SDK. These classes are designed for JSON
 * serialization with Jackson and provide fluent setter methods for convenient
 * configuration.
 *
 * <h2>Client Configuration</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.CopilotClientOptions} - Options for
 * configuring the {@link com.github.copilot.sdk.CopilotClient}, including CLI
 * path, port, transport mode, and auto-start behavior.</li>
 * </ul>
 *
 * <h2>Session Configuration</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.SessionConfig} - Configuration for
 * creating a new session, including model selection, tools, system message, and
 * MCP server configuration.</li>
 * <li>{@link com.github.copilot.sdk.json.ResumeSessionConfig} - Configuration
 * for resuming an existing session.</li>
 * <li>{@link com.github.copilot.sdk.json.InfiniteSessionConfig} - Configuration
 * for infinite sessions with automatic context compaction.</li>
 * <li>{@link com.github.copilot.sdk.json.SystemMessageConfig} - System message
 * customization options.</li>
 * </ul>
 *
 * <h2>Message and Tool Configuration</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.MessageOptions} - Options for sending
 * messages, including prompt text and attachments.</li>
 * <li>{@link com.github.copilot.sdk.json.ToolDefinition} - Definition of a
 * custom tool that can be invoked by the assistant.</li>
 * <li>{@link com.github.copilot.sdk.json.ToolInvocation} - Represents a tool
 * invocation request from the assistant.</li>
 * <li>{@link com.github.copilot.sdk.json.Attachment} - File attachment for
 * messages.</li>
 * </ul>
 *
 * <h2>Provider Configuration (BYOK)</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.ProviderConfig} - Configuration for
 * using your own API keys with custom providers (OpenAI, Azure, etc.).</li>
 * <li>{@link com.github.copilot.sdk.json.AzureOptions} - Azure-specific
 * configuration options.</li>
 * </ul>
 *
 * <h2>Model Information</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.ModelInfo} - Information about an
 * available AI model.</li>
 * <li>{@link com.github.copilot.sdk.json.ModelCapabilities} - Model
 * capabilities and limits.</li>
 * <li>{@link com.github.copilot.sdk.json.ModelPolicy} - Model policy and state
 * information.</li>
 * </ul>
 *
 * <h2>Custom Agents</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.CustomAgentConfig} - Configuration for
 * custom agents with specialized behaviors and tools.</li>
 * </ul>
 *
 * <h2>Permissions</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.json.PermissionHandler} - Handler for
 * permission requests from the assistant.</li>
 * <li>{@link com.github.copilot.sdk.json.PermissionRequest} - A permission
 * request from the assistant.</li>
 * <li>{@link com.github.copilot.sdk.json.PermissionRequestResult} - Result of a
 * permission request decision.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * var config = new SessionConfig().setModel("gpt-4.1").setStreaming(true)
 * 		.setSystemMessage(new SystemMessageConfig().setMode(SystemMessageMode.APPEND)
 * 				.setContent("Be concise in your responses."))
 * 		.setTools(List.of(ToolDefinition.create("my_tool", "Description", schema, handler)));
 *
 * var session = client.createSession(config).get();
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotClient
 * @see com.github.copilot.sdk.CopilotSession
 */
package com.github.copilot.sdk.json;

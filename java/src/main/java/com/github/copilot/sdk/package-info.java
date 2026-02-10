/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Core classes for the GitHub Copilot SDK for Java.
 *
 * <p>
 * This package provides the main entry points for interacting with GitHub
 * Copilot programmatically. The SDK enables Java applications to leverage
 * Copilot's agentic capabilities, including multi-turn conversations, tool
 * execution, and AI-powered code generation.
 *
 * <h2>Main Classes</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.CopilotClient} - The main client for
 * connecting to and communicating with the Copilot CLI. Manages the lifecycle
 * of the CLI process and provides methods for creating sessions, querying
 * models, and checking authentication status.</li>
 * <li>{@link com.github.copilot.sdk.CopilotSession} - Represents a single
 * conversation session with Copilot. Sessions maintain context across multiple
 * messages and support streaming responses, tool invocations, and event
 * handling.</li>
 * <li>{@link com.github.copilot.sdk.JsonRpcClient} - Low-level JSON-RPC client
 * for communication with the Copilot CLI process.</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * try (var client = new CopilotClient()) {
 * 	client.start().get();
 *
 * 	var session = client.createSession(new SessionConfig().setModel("gpt-4.1")).get();
 *
 * 	session.on(AssistantMessageEvent.class, msg -> {
 * 		System.out.println(msg.getData().content());
 * 	});
 *
 * 	session.send(new MessageOptions().setPrompt("Hello, Copilot!")).get();
 * }
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 * <li>{@link com.github.copilot.sdk.events} - Event types emitted during
 * session processing</li>
 * <li>{@link com.github.copilot.sdk.json} - Configuration and data transfer
 * objects</li>
 * </ul>
 *
 * @see com.github.copilot.sdk.CopilotClient
 * @see com.github.copilot.sdk.CopilotSession
 * @see <a href=
 *      "https://github.com/copilot-community-sdk/copilot-sdk-java">GitHub
 *      Repository</a>
 */
package com.github.copilot.sdk;

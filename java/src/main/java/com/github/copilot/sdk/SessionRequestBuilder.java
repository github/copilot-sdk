/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.copilot.sdk.json.CreateSessionRequest;
import com.github.copilot.sdk.json.CommandWireDefinition;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.ResumeSessionRequest;
import com.github.copilot.sdk.json.SectionOverride;
import com.github.copilot.sdk.json.SectionOverrideAction;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;

/**
 * Builds JSON-RPC request objects from session configuration.
 * <p>
 * This class handles the conversion of SDK configuration objects
 * ({@link SessionConfig}, {@link ResumeSessionConfig}) to JSON-RPC request
 * objects for session creation and resumption.
 */
final class SessionRequestBuilder {

    private SessionRequestBuilder() {
        // Utility class
    }

    /**
     * Extracts transform callbacks from a {@link SystemMessageConfig} and returns a
     * wire-safe copy of the config alongside the extracted callbacks.
     * <p>
     * When the system message mode is {@link SystemMessageMode#CUSTOMIZE} and some
     * sections have {@link SectionOverride#getTransform() transform} callbacks set,
     * this method:
     * <ol>
     * <li>Removes the callbacks from the wire config (they must not be
     * serialized).</li>
     * <li>Replaces each transform section with
     * {@link SectionOverrideAction#TRANSFORM} in the wire config.</li>
     * <li>Returns the callbacks so they can be registered with the session.</li>
     * </ol>
     *
     * @param systemMessage
     *            the system message config, may be {@code null}
     * @return an {@link ExtractedTransforms} containing the wire-safe config and
     *         any extracted callbacks
     */
    static ExtractedTransforms extractTransformCallbacks(SystemMessageConfig systemMessage) {
        if (systemMessage == null || systemMessage.getMode() != SystemMessageMode.CUSTOMIZE
                || systemMessage.getSections() == null) {
            return new ExtractedTransforms(systemMessage, null);
        }

        Map<String, Function<String, CompletableFuture<String>>> callbacks = new HashMap<>();
        Map<String, SectionOverride> wireSections = new HashMap<>();

        for (Map.Entry<String, SectionOverride> entry : systemMessage.getSections().entrySet()) {
            String sectionId = entry.getKey();
            SectionOverride override = entry.getValue();

            if (override.getTransform() != null) {
                callbacks.put(sectionId, override.getTransform());
                wireSections.put(sectionId, new SectionOverride().setAction(SectionOverrideAction.TRANSFORM));
            } else {
                wireSections.put(sectionId, override);
            }
        }

        if (callbacks.isEmpty()) {
            return new ExtractedTransforms(systemMessage, null);
        }

        // Build a wire-safe copy of the system message with callbacks removed
        var wireConfig = new SystemMessageConfig().setMode(systemMessage.getMode())
                .setContent(systemMessage.getContent()).setSections(wireSections);

        return new ExtractedTransforms(wireConfig, callbacks);
    }

    /**
     * Builds a CreateSessionRequest from the given configuration.
     *
     * @param config
     *            the session configuration (may be null)
     * @param sessionId
     *            the pre-generated session ID to use
     * @return the built request object
     */
    static CreateSessionRequest buildCreateRequest(SessionConfig config, String sessionId) {
        var request = new CreateSessionRequest();
        // Always request permission callbacks to enable deny-by-default behavior
        request.setRequestPermission(true);
        // Always send envValueMode=direct for MCP servers
        request.setEnvValueMode("direct");
        request.setSessionId(sessionId);
        if (config == null) {
            return request;
        }

        request.setModel(config.getModel());
        request.setClientName(config.getClientName());
        request.setReasoningEffort(config.getReasoningEffort());
        request.setTools(config.getTools());
        request.setSystemMessage(config.getSystemMessage());
        request.setAvailableTools(config.getAvailableTools());
        request.setExcludedTools(config.getExcludedTools());
        request.setProvider(config.getProvider());
        request.setRequestUserInput(config.getOnUserInputRequest() != null ? true : null);
        request.setHooks(config.getHooks() != null && config.getHooks().hasHooks() ? true : null);
        request.setWorkingDirectory(config.getWorkingDirectory());
        request.setStreaming(config.isStreaming() ? true : null);
        request.setIncludeSubAgentStreamingEvents(config.getIncludeSubAgentStreamingEvents());
        request.setMcpServers(config.getMcpServers());
        request.setCustomAgents(config.getCustomAgents());
        request.setAgent(config.getAgent());
        request.setInfiniteSessions(config.getInfiniteSessions());
        request.setSkillDirectories(config.getSkillDirectories());
        request.setDisabledSkills(config.getDisabledSkills());
        request.setConfigDir(config.getConfigDir());
        request.setEnableConfigDiscovery(config.getEnableConfigDiscovery());
        request.setModelCapabilities(config.getModelCapabilities());

        if (config.getCommands() != null && !config.getCommands().isEmpty()) {
            var wireCommands = config.getCommands().stream()
                    .map(c -> new CommandWireDefinition(c.getName(), c.getDescription()))
                    .collect(java.util.stream.Collectors.toList());
            request.setCommands(wireCommands);
        }
        if (config.getOnElicitationRequest() != null) {
            request.setRequestElicitation(true);
        }

        return request;
    }

    /**
     * Builds a CreateSessionRequest from the given configuration.
     *
     * @param config
     *            the session configuration (may be null)
     * @return the built request object
     * @deprecated Use {@link #buildCreateRequest(SessionConfig, String)} instead.
     */
    @Deprecated
    static CreateSessionRequest buildCreateRequest(SessionConfig config) {
        String sessionId = (config != null && config.getSessionId() != null)
                ? config.getSessionId()
                : java.util.UUID.randomUUID().toString();
        return buildCreateRequest(config, sessionId);
    }

    /**
     * Builds a ResumeSessionRequest from the given session ID and configuration.
     *
     * @param sessionId
     *            the ID of the session to resume
     * @param config
     *            the resume configuration (may be null)
     * @return the built request object
     */
    static ResumeSessionRequest buildResumeRequest(String sessionId, ResumeSessionConfig config) {
        var request = new ResumeSessionRequest();
        request.setSessionId(sessionId);
        // Always request permission callbacks to enable deny-by-default behavior
        request.setRequestPermission(true);
        // Always send envValueMode=direct for MCP servers
        request.setEnvValueMode("direct");

        if (config == null) {
            return request;
        }

        request.setModel(config.getModel());
        request.setClientName(config.getClientName());
        request.setReasoningEffort(config.getReasoningEffort());
        request.setTools(config.getTools());
        request.setSystemMessage(config.getSystemMessage());
        request.setAvailableTools(config.getAvailableTools());
        request.setExcludedTools(config.getExcludedTools());
        request.setProvider(config.getProvider());
        request.setRequestUserInput(config.getOnUserInputRequest() != null ? true : null);
        request.setHooks(config.getHooks() != null && config.getHooks().hasHooks() ? true : null);
        request.setWorkingDirectory(config.getWorkingDirectory());
        request.setConfigDir(config.getConfigDir());
        request.setEnableConfigDiscovery(config.getEnableConfigDiscovery());
        request.setDisableResume(config.isDisableResume() ? true : null);
        request.setStreaming(config.isStreaming() ? true : null);
        request.setIncludeSubAgentStreamingEvents(config.getIncludeSubAgentStreamingEvents());
        request.setMcpServers(config.getMcpServers());
        request.setCustomAgents(config.getCustomAgents());
        request.setAgent(config.getAgent());
        request.setSkillDirectories(config.getSkillDirectories());
        request.setDisabledSkills(config.getDisabledSkills());
        request.setInfiniteSessions(config.getInfiniteSessions());
        request.setModelCapabilities(config.getModelCapabilities());

        if (config.getCommands() != null && !config.getCommands().isEmpty()) {
            var wireCommands = config.getCommands().stream()
                    .map(c -> new CommandWireDefinition(c.getName(), c.getDescription()))
                    .collect(java.util.stream.Collectors.toList());
            request.setCommands(wireCommands);
        }
        if (config.getOnElicitationRequest() != null) {
            request.setRequestElicitation(true);
        }

        return request;
    }

    /**
     * Configures a session with handlers from the given config.
     *
     * @param session
     *            the session to configure
     * @param config
     *            the session configuration
     */
    static void configureSession(CopilotSession session, SessionConfig config) {
        if (config == null) {
            return;
        }

        if (config.getTools() != null) {
            session.registerTools(config.getTools());
        }
        if (config.getOnPermissionRequest() != null) {
            session.registerPermissionHandler(config.getOnPermissionRequest());
        }
        if (config.getOnUserInputRequest() != null) {
            session.registerUserInputHandler(config.getOnUserInputRequest());
        }
        if (config.getHooks() != null) {
            session.registerHooks(config.getHooks());
        }
        if (config.getCommands() != null) {
            session.registerCommands(config.getCommands());
        }
        if (config.getOnElicitationRequest() != null) {
            session.registerElicitationHandler(config.getOnElicitationRequest());
        }
        if (config.getOnEvent() != null) {
            session.on(config.getOnEvent());
        }
    }

    /**
     * Configures a resumed session with handlers from the given config.
     *
     * @param session
     *            the session to configure
     * @param config
     *            the resume session configuration
     */
    static void configureSession(CopilotSession session, ResumeSessionConfig config) {
        if (config == null) {
            return;
        }

        if (config.getTools() != null) {
            session.registerTools(config.getTools());
        }
        if (config.getOnPermissionRequest() != null) {
            session.registerPermissionHandler(config.getOnPermissionRequest());
        }
        if (config.getOnUserInputRequest() != null) {
            session.registerUserInputHandler(config.getOnUserInputRequest());
        }
        if (config.getHooks() != null) {
            session.registerHooks(config.getHooks());
        }
        if (config.getCommands() != null) {
            session.registerCommands(config.getCommands());
        }
        if (config.getOnElicitationRequest() != null) {
            session.registerElicitationHandler(config.getOnElicitationRequest());
        }
        if (config.getOnEvent() != null) {
            session.on(config.getOnEvent());
        }
    }
}

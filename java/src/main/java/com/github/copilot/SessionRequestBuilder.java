/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.copilot.rpc.CreateSessionRequest;
import com.github.copilot.rpc.CommandWireDefinition;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.ResumeSessionRequest;
import com.github.copilot.rpc.SectionOverride;
import com.github.copilot.rpc.SectionOverrideAction;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;

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
        return buildCreateRequest(config, sessionId, CopilotClientMode.COPILOT_CLI);
    }

    static CreateSessionRequest buildCreateRequest(SessionConfig config, String sessionId, CopilotClientMode mode) {
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
        request.setReasoningSummary(config.getReasoningSummary());
        request.setContextTier(config.getContextTier());
        request.setTools(config.getTools());
        request.setSystemMessage(config.getSystemMessage());
        request.setAvailableTools(config.getAvailableTools());
        request.setExcludedTools(config.getExcludedTools());
        request.setProvider(config.getProvider());
        config.getEnableSessionTelemetry().ifPresent(request::setEnableSessionTelemetry);
        experimentalModeForMode(mode, config.getEnableExperimentalMode().orElse(null))
                .ifPresent(request::setIsExperimentalMode);
        if (config.getOnUserInputRequest() != null) {
            request.setRequestUserInput(true);
        }
        if (config.getHooks() != null && config.getHooks().hasHooks()) {
            request.setHooks(true);
        }
        request.setWorkingDirectory(config.getWorkingDirectory());
        if (config.isStreaming()) {
            request.setStreaming(true);
        }
        config.getIncludeSubAgentStreamingEvents().ifPresent(request::setIncludeSubAgentStreamingEvents);
        request.setMcpServers(config.getMcpServers());
        request.setMcpOAuthTokenStorage(config.getMcpOAuthTokenStorage());
        request.setCustomAgents(config.getCustomAgents());
        request.setDefaultAgent(config.getDefaultAgent());
        request.setAgent(config.getAgent());
        request.setInfiniteSessions(config.getInfiniteSessions());
        request.setSkillDirectories(config.getSkillDirectories());
        request.setInstructionDirectories(config.getInstructionDirectories());
        request.setPluginDirectories(config.getPluginDirectories());
        request.setLargeOutput(config.getLargeOutput());
        request.setDisabledSkills(config.getDisabledSkills());
        request.setConfigDirectory(config.getConfigDirectory());
        config.getEnableConfigDiscovery().ifPresent(request::setEnableConfigDiscovery);
        config.getSkipEmbeddingRetrieval().ifPresent(request::setSkipEmbeddingRetrieval);
        if (config.getOrganizationCustomInstructions() != null) {
            request.setOrganizationCustomInstructions(config.getOrganizationCustomInstructions());
        }
        config.getEnableOnDemandInstructionDiscovery().ifPresent(request::setEnableOnDemandInstructionDiscovery);
        config.getEnableFileHooks().ifPresent(request::setEnableFileHooks);
        config.getEnableHostGitOperations().ifPresent(request::setEnableHostGitOperations);
        config.getEnableSessionStore().ifPresent(request::setEnableSessionStore);
        config.getEnableSkills().ifPresent(request::setEnableSkills);
        if (config.getEmbeddingCacheStorage() != null) {
            request.setEmbeddingCacheStorage(config.getEmbeddingCacheStorage());
        }
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
        if (config.isEnableMcpApps()) {
            request.setRequestMcpApps(true);
        }
        if (config.getOnExitPlanMode() != null) {
            request.setRequestExitPlanMode(true);
        }
        if (config.getOnAutoModeSwitch() != null) {
            request.setRequestAutoModeSwitch(true);
        }
        request.setGitHubToken(config.getGitHubToken());
        request.setRemoteSession(config.getRemoteSession());
        request.setCloud(config.getCloud());

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
        return buildResumeRequest(sessionId, config, CopilotClientMode.COPILOT_CLI);
    }

    static ResumeSessionRequest buildResumeRequest(String sessionId, ResumeSessionConfig config,
            CopilotClientMode mode) {
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
        request.setReasoningSummary(config.getReasoningSummary());
        request.setContextTier(config.getContextTier());
        request.setTools(config.getTools());
        request.setSystemMessage(config.getSystemMessage());
        request.setAvailableTools(config.getAvailableTools());
        request.setExcludedTools(config.getExcludedTools());
        request.setProvider(config.getProvider());
        config.getEnableSessionTelemetry().ifPresent(request::setEnableSessionTelemetry);
        experimentalModeForMode(mode, config.getEnableExperimentalMode().orElse(null))
                .ifPresent(request::setIsExperimentalMode);
        if (config.getOnUserInputRequest() != null) {
            request.setRequestUserInput(true);
        }
        if (config.getHooks() != null && config.getHooks().hasHooks()) {
            request.setHooks(true);
        }
        request.setWorkingDirectory(config.getWorkingDirectory());
        request.setConfigDirectory(config.getConfigDirectory());
        config.getEnableConfigDiscovery().ifPresent(request::setEnableConfigDiscovery);
        config.getSkipEmbeddingRetrieval().ifPresent(request::setSkipEmbeddingRetrieval);
        if (config.getOrganizationCustomInstructions() != null) {
            request.setOrganizationCustomInstructions(config.getOrganizationCustomInstructions());
        }
        config.getEnableOnDemandInstructionDiscovery().ifPresent(request::setEnableOnDemandInstructionDiscovery);
        config.getEnableFileHooks().ifPresent(request::setEnableFileHooks);
        config.getEnableHostGitOperations().ifPresent(request::setEnableHostGitOperations);
        config.getEnableSessionStore().ifPresent(request::setEnableSessionStore);
        config.getEnableSkills().ifPresent(request::setEnableSkills);
        if (config.getEmbeddingCacheStorage() != null) {
            request.setEmbeddingCacheStorage(config.getEmbeddingCacheStorage());
        }
        if (config.isDisableResume()) {
            request.setDisableResume(true);
        }
        if (config.isStreaming()) {
            request.setStreaming(true);
        }
        config.getIncludeSubAgentStreamingEvents().ifPresent(request::setIncludeSubAgentStreamingEvents);
        request.setMcpServers(config.getMcpServers());
        request.setMcpOAuthTokenStorage(config.getMcpOAuthTokenStorage());
        request.setCustomAgents(config.getCustomAgents());
        request.setDefaultAgent(config.getDefaultAgent());
        request.setAgent(config.getAgent());
        request.setSkillDirectories(config.getSkillDirectories());
        request.setInstructionDirectories(config.getInstructionDirectories());
        request.setPluginDirectories(config.getPluginDirectories());
        request.setLargeOutput(config.getLargeOutput());
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
        if (config.isEnableMcpApps()) {
            request.setRequestMcpApps(true);
        }
        if (config.getOnExitPlanMode() != null) {
            request.setRequestExitPlanMode(true);
        }
        if (config.getOnAutoModeSwitch() != null) {
            request.setRequestAutoModeSwitch(true);
        }
        request.setGitHubToken(config.getGitHubToken());
        request.setRemoteSession(config.getRemoteSession());

        return request;
    }

    private static Optional<Boolean> experimentalModeForMode(CopilotClientMode mode, Boolean supplied) {
        if (mode == CopilotClientMode.EMPTY) {
            return Optional.of(supplied != null ? supplied : false);
        }
        return Optional.ofNullable(supplied);
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
        if (config.getOnExitPlanMode() != null) {
            session.registerExitPlanModeHandler(config.getOnExitPlanMode());
        }
        if (config.getOnAutoModeSwitch() != null) {
            session.registerAutoModeSwitchHandler(config.getOnAutoModeSwitch());
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
        if (config.getOnExitPlanMode() != null) {
            session.registerExitPlanModeHandler(config.getOnExitPlanMode());
        }
        if (config.getOnAutoModeSwitch() != null) {
            session.registerAutoModeSwitchHandler(config.getOnAutoModeSwitch());
        }
        if (config.getOnEvent() != null) {
            session.on(config.getOnEvent());
        }
    }
}

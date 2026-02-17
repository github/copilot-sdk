/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import com.github.copilot.sdk.json.CreateSessionRequest;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.ResumeSessionRequest;
import com.github.copilot.sdk.json.SessionConfig;

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
     * Builds a CreateSessionRequest from the given configuration.
     *
     * @param config
     *            the session configuration (may be null)
     * @return the built request object
     */
    static CreateSessionRequest buildCreateRequest(SessionConfig config) {
        var request = new CreateSessionRequest();
        if (config == null) {
            return request;
        }

        request.setModel(config.getModel());
        request.setSessionId(config.getSessionId());
        request.setReasoningEffort(config.getReasoningEffort());
        request.setTools(config.getTools());
        request.setSystemMessage(config.getSystemMessage());
        request.setAvailableTools(config.getAvailableTools());
        request.setExcludedTools(config.getExcludedTools());
        request.setProvider(config.getProvider());
        request.setRequestPermission(config.getOnPermissionRequest() != null ? true : null);
        request.setRequestUserInput(config.getOnUserInputRequest() != null ? true : null);
        request.setHooks(config.getHooks() != null && config.getHooks().hasHooks() ? true : null);
        request.setWorkingDirectory(config.getWorkingDirectory());
        request.setStreaming(config.isStreaming() ? true : null);
        request.setMcpServers(config.getMcpServers());
        request.setEnvValueMode("direct");
        request.setCustomAgents(config.getCustomAgents());
        request.setInfiniteSessions(config.getInfiniteSessions());
        request.setSkillDirectories(config.getSkillDirectories());
        request.setDisabledSkills(config.getDisabledSkills());
        request.setConfigDir(config.getConfigDir());

        return request;
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

        if (config == null) {
            return request;
        }

        request.setModel(config.getModel());
        request.setReasoningEffort(config.getReasoningEffort());
        request.setTools(config.getTools());
        request.setSystemMessage(config.getSystemMessage());
        request.setAvailableTools(config.getAvailableTools());
        request.setExcludedTools(config.getExcludedTools());
        request.setProvider(config.getProvider());
        request.setRequestPermission(config.getOnPermissionRequest() != null ? true : null);
        request.setRequestUserInput(config.getOnUserInputRequest() != null ? true : null);
        request.setHooks(config.getHooks() != null && config.getHooks().hasHooks() ? true : null);
        request.setWorkingDirectory(config.getWorkingDirectory());
        request.setConfigDir(config.getConfigDir());
        request.setDisableResume(config.isDisableResume() ? true : null);
        request.setStreaming(config.isStreaming() ? true : null);
        request.setMcpServers(config.getMcpServers());
        request.setEnvValueMode("direct");
        request.setCustomAgents(config.getCustomAgents());
        request.setSkillDirectories(config.getSkillDirectories());
        request.setDisabledSkills(config.getDisabledSkills());
        request.setInfiniteSessions(config.getInfiniteSessions());

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
    }
}

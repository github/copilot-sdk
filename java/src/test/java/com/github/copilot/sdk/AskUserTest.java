/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.UserInputRequest;
import com.github.copilot.sdk.json.UserInputResponse;

/**
 * Tests for user input handler (ask_user) functionality.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/ask_user/.
 * </p>
 */
public class AskUserTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    /**
     * Verifies that user input handler is invoked when model uses ask_user tool.
     *
     * @see Snapshot:
     *      ask_user/should_invoke_user_input_handler_when_model_uses_ask_user_tool
     */
    @Test
    void testShouldInvokeUserInputHandlerWhenModelUsesAskUserTool() throws Exception {
        ctx.configureForTest("ask_user", "should_invoke_user_input_handler_when_model_uses_ask_user_tool");

        List<UserInputRequest> userInputRequests = new ArrayList<>();
        final String[] sessionIdHolder = new String[1];

        SessionConfig config = new SessionConfig().setOnUserInputRequest((request, invocation) -> {
            userInputRequests.add(request);
            assertEquals(sessionIdHolder[0], invocation.getSessionId());

            // Return the first choice if available, otherwise a freeform answer
            String answer = (request.getChoices() != null && !request.getChoices().isEmpty())
                    ? request.getChoices().get(0)
                    : "freeform answer";
            boolean wasFreeform = request.getChoices() == null || request.getChoices().isEmpty();

            return CompletableFuture
                    .completedFuture(new UserInputResponse().setAnswer(answer).setWasFreeform(wasFreeform));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();
            sessionIdHolder[0] = session.getSessionId();

            session.sendAndWait(new MessageOptions().setPrompt(
                    "Ask me to choose between 'Option A' and 'Option B' using the ask_user tool. Wait for my response before continuing."))
                    .get(60, TimeUnit.SECONDS);

            // Should have received at least one user input request
            assertFalse(userInputRequests.isEmpty(), "Should have received user input requests");

            // The request should have a question
            assertTrue(userInputRequests.stream().anyMatch(r -> r.getQuestion() != null && !r.getQuestion().isEmpty()),
                    "User input request should have a question");
        }
    }

    /**
     * Verifies that choices are received in user input requests.
     *
     * @see Snapshot: ask_user/should_receive_choices_in_user_input_request
     */
    @Test
    void testShouldReceiveChoicesInUserInputRequest() throws Exception {
        ctx.configureForTest("ask_user", "should_receive_choices_in_user_input_request");

        List<UserInputRequest> userInputRequests = new ArrayList<>();

        SessionConfig config = new SessionConfig().setOnUserInputRequest((request, invocation) -> {
            userInputRequests.add(request);

            // Pick the first choice
            String answer = (request.getChoices() != null && !request.getChoices().isEmpty())
                    ? request.getChoices().get(0)
                    : "default";

            return CompletableFuture.completedFuture(new UserInputResponse().setAnswer(answer).setWasFreeform(false));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the ask_user tool to ask me to pick between exactly two options: 'Red' and 'Blue'. These should be provided as choices. Wait for my answer."))
                    .get(60, TimeUnit.SECONDS);

            // Should have received a request
            assertFalse(userInputRequests.isEmpty(), "Should have received user input requests");

            // At least one request should have choices
            assertTrue(userInputRequests.stream().anyMatch(r -> r.getChoices() != null && !r.getChoices().isEmpty()),
                    "At least one request should have choices");
        }
    }

    /**
     * Verifies that freeform user input responses are handled.
     *
     * @see Snapshot: ask_user/should_handle_freeform_user_input_response
     */
    @Test
    void testShouldHandleFreeformUserInputResponse() throws Exception {
        ctx.configureForTest("ask_user", "should_handle_freeform_user_input_response");

        final List<UserInputRequest> userInputRequests = new ArrayList<>();
        String freeformAnswer = "This is my custom freeform answer that was not in the choices";

        SessionConfig config = new SessionConfig().setOnUserInputRequest((request, invocation) -> {
            userInputRequests.add(request);

            // Return a freeform answer (not from choices)
            return CompletableFuture
                    .completedFuture(new UserInputResponse().setAnswer(freeformAnswer).setWasFreeform(true));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            var response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Ask me a question using ask_user and then include my answer in your response. The question should be 'What is your favorite color?'"))
                    .get(60, TimeUnit.SECONDS);

            // Should have received a request
            assertFalse(userInputRequests.isEmpty(), "Should have received user input requests");

            // The model's response should be defined
            assertNotNull(response, "Response should not be null");
        }
    }
}

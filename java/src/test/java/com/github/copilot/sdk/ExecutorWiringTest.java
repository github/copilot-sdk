/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.UserInputResponse;

/**
 * TDD red-phase tests verifying that when an {@link Executor} is provided via
 * {@link CopilotClientOptions#setExecutor(Executor)}, all internal
 * {@code CompletableFuture.*Async} calls are routed through that executor
 * instead of {@code ForkJoinPool.commonPool()}.
 *
 * <p>
 * Uses a {@link TrackingExecutor} decorator that delegates to a real executor
 * while counting task submissions. After SDK operations complete, the tests
 * assert the decorator was invoked.
 * </p>
 */
public class ExecutorWiringTest {

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
     * A decorator executor that delegates to a real executor while counting task
     * submissions.
     */
    static class TrackingExecutor implements Executor {

        private final Executor delegate;
        private final AtomicInteger taskCount = new AtomicInteger(0);

        TrackingExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            taskCount.incrementAndGet();
            delegate.execute(command);
        }

        int getTaskCount() {
            return taskCount.get();
        }
    }

    private CopilotClientOptions createOptionsWithExecutor(TrackingExecutor executor) {
        CopilotClientOptions options = new CopilotClientOptions().setCliPath(ctx.getCliPath())
                .setCwd(ctx.getWorkDir().toString()).setEnvironment(ctx.getEnvironment()).setExecutor(executor);

        String ci = System.getenv("GITHUB_ACTIONS");
        if (ci != null && !ci.isEmpty()) {
            options.setGitHubToken("fake-token-for-e2e-tests");
        }
        return options;
    }

    /**
     * Verifies that client start-up routes through the provided executor.
     *
     * <p>
     * {@code CopilotClient.startCore()} uses
     * {@code CompletableFuture.supplyAsync(...)} to initialize the connection. This
     * test asserts that the start-up task goes through the caller-supplied
     * executor, not {@code ForkJoinPool.commonPool()}.
     * </p>
     *
     * @see Snapshot: tools/invokes_custom_tool
     */
    @Test
    void testClientStartUsesProvidedExecutor() throws Exception {
        ctx.configureForTest("tools", "invokes_custom_tool");

        TrackingExecutor trackingExecutor = new TrackingExecutor(ForkJoinPool.commonPool());
        int beforeStart = trackingExecutor.getTaskCount();

        try (CopilotClient client = new CopilotClient(createOptionsWithExecutor(trackingExecutor))) {
            client.start().get(30, TimeUnit.SECONDS);

            assertTrue(trackingExecutor.getTaskCount() > beforeStart,
                    "Expected the tracking executor to have been invoked during client start, "
                            + "but task count did not increase. CopilotClient.startCore() is not "
                            + "routing supplyAsync through the provided executor.");
        }
    }

    /**
     * Verifies that tool call dispatch routes through the provided executor.
     *
     * <p>
     * When a custom tool is invoked by the LLM, the {@code RpcHandlerDispatcher}
     * calls {@code CompletableFuture.runAsync(...)} to dispatch the tool handler.
     * This test asserts that dispatch goes through the caller-supplied executor.
     * </p>
     *
     * @see Snapshot: tools/invokes_custom_tool
     */
    @Test
    void testToolCallDispatchUsesProvidedExecutor() throws Exception {
        ctx.configureForTest("tools", "invokes_custom_tool");

        TrackingExecutor trackingExecutor = new TrackingExecutor(ForkJoinPool.commonPool());

        var parameters = new HashMap<String, Object>();
        var properties = new HashMap<String, Object>();
        var inputProp = new HashMap<String, Object>();
        inputProp.put("type", "string");
        inputProp.put("description", "String to encrypt");
        properties.put("input", inputProp);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("input"));

        ToolDefinition encryptTool = ToolDefinition.create("encrypt_string", "Encrypts a string", parameters,
                (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    String input = (String) args.get("input");
                    return CompletableFuture.completedFuture(input.toUpperCase());
                });

        // Reset count after client construction to isolate tool-call dispatch
        try (CopilotClient client = new CopilotClient(createOptionsWithExecutor(trackingExecutor))) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(encryptTool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            int beforeToolCall = trackingExecutor.getTaskCount();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Use encrypt_string to encrypt this string: Hello"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);

            assertTrue(trackingExecutor.getTaskCount() > beforeToolCall,
                    "Expected the tracking executor to have been invoked for tool call dispatch, "
                            + "but task count did not increase after sendAndWait. "
                            + "RpcHandlerDispatcher is not routing runAsync through the provided executor.");

            session.close();
        }
    }

    /**
     * Verifies that permission request dispatch routes through the provided
     * executor.
     *
     * <p>
     * When the LLM requests a permission, the {@code RpcHandlerDispatcher} calls
     * {@code CompletableFuture.runAsync(...)} to dispatch the permission handler.
     * This test asserts that dispatch goes through the caller-supplied executor.
     * </p>
     *
     * @see Snapshot: permissions/permission_handler_for_write_operations
     */
    @Test
    void testPermissionDispatchUsesProvidedExecutor() throws Exception {
        ctx.configureForTest("permissions", "permission_handler_for_write_operations");

        TrackingExecutor trackingExecutor = new TrackingExecutor(ForkJoinPool.commonPool());

        var config = new SessionConfig().setOnPermissionRequest((request, invocation) -> CompletableFuture
                .completedFuture(new PermissionRequestResult().setKind("approved")));

        try (CopilotClient client = new CopilotClient(createOptionsWithExecutor(trackingExecutor))) {
            CopilotSession session = client.createSession(config).get();

            Path testFile = ctx.getWorkDir().resolve("test.txt");
            Files.writeString(testFile, "original content");

            int beforeSend = trackingExecutor.getTaskCount();

            session.sendAndWait(new MessageOptions().setPrompt("Edit test.txt and replace 'original' with 'modified'"))
                    .get(60, TimeUnit.SECONDS);

            assertTrue(trackingExecutor.getTaskCount() > beforeSend,
                    "Expected the tracking executor to have been invoked for permission dispatch, "
                            + "but task count did not increase after sendAndWait. "
                            + "RpcHandlerDispatcher is not routing permission runAsync through the provided executor.");

            session.close();
        }
    }

    /**
     * Verifies that user input request dispatch routes through the provided
     * executor.
     *
     * <p>
     * When the LLM asks for user input, the {@code RpcHandlerDispatcher} calls
     * {@code CompletableFuture.runAsync(...)} to dispatch the user input handler.
     * This test asserts that dispatch goes through the caller-supplied executor.
     * </p>
     *
     * @see Snapshot:
     *      ask_user/should_invoke_user_input_handler_when_model_uses_ask_user_tool
     */
    @Test
    void testUserInputDispatchUsesProvidedExecutor() throws Exception {
        ctx.configureForTest("ask_user", "should_invoke_user_input_handler_when_model_uses_ask_user_tool");

        TrackingExecutor trackingExecutor = new TrackingExecutor(ForkJoinPool.commonPool());

        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setOnUserInputRequest((request, invocation) -> {
                    String answer = (request.getChoices() != null && !request.getChoices().isEmpty())
                            ? request.getChoices().get(0)
                            : "freeform answer";
                    boolean wasFreeform = request.getChoices() == null || request.getChoices().isEmpty();
                    return CompletableFuture
                            .completedFuture(new UserInputResponse().setAnswer(answer).setWasFreeform(wasFreeform));
                });

        try (CopilotClient client = new CopilotClient(createOptionsWithExecutor(trackingExecutor))) {
            CopilotSession session = client.createSession(config).get();

            int beforeSend = trackingExecutor.getTaskCount();

            session.sendAndWait(new MessageOptions().setPrompt(
                    "Ask me to choose between 'Option A' and 'Option B' using the ask_user tool. Wait for my response before continuing."))
                    .get(60, TimeUnit.SECONDS);

            assertTrue(trackingExecutor.getTaskCount() > beforeSend,
                    "Expected the tracking executor to have been invoked for user input dispatch, "
                            + "but task count did not increase after sendAndWait. "
                            + "RpcHandlerDispatcher is not routing userInput runAsync through the provided executor.");

            session.close();
        }
    }

    /**
     * Verifies that hooks dispatch routes through the provided executor.
     *
     * <p>
     * When the LLM triggers a hook, the {@code RpcHandlerDispatcher} calls
     * {@code CompletableFuture.runAsync(...)} to dispatch the hooks handler. This
     * test asserts that dispatch goes through the caller-supplied executor.
     * </p>
     *
     * @see Snapshot: hooks/invoke_pre_tool_use_hook_when_model_runs_a_tool
     */
    @Test
    void testHooksDispatchUsesProvidedExecutor() throws Exception {
        ctx.configureForTest("hooks", "invoke_pre_tool_use_hook_when_model_runs_a_tool");

        TrackingExecutor trackingExecutor = new TrackingExecutor(ForkJoinPool.commonPool());

        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setHooks(new SessionHooks().setOnPreToolUse(
                        (input, invocation) -> CompletableFuture.completedFuture(PreToolUseHookOutput.allow())));

        try (CopilotClient client = new CopilotClient(createOptionsWithExecutor(trackingExecutor))) {
            CopilotSession session = client.createSession(config).get();

            Path testFile = ctx.getWorkDir().resolve("hello.txt");
            Files.writeString(testFile, "Hello from the test!");

            int beforeSend = trackingExecutor.getTaskCount();

            session.sendAndWait(
                    new MessageOptions().setPrompt("Read the contents of hello.txt and tell me what it says"))
                    .get(60, TimeUnit.SECONDS);

            assertTrue(trackingExecutor.getTaskCount() > beforeSend,
                    "Expected the tracking executor to have been invoked for hooks dispatch, "
                            + "but task count did not increase after sendAndWait. "
                            + "RpcHandlerDispatcher is not routing hooks runAsync through the provided executor.");

            session.close();
        }
    }

    /**
     * Verifies that {@code CopilotClient.stop()} routes session closure through the
     * provided executor.
     *
     * <p>
     * {@code CopilotClient.stop()} uses {@code CompletableFuture.runAsync(...)} to
     * close each active session. This test asserts that those closures go through
     * the caller-supplied executor.
     * </p>
     *
     * @see Snapshot: tools/invokes_custom_tool
     */
    @Test
    void testClientStopUsesProvidedExecutor() throws Exception {
        ctx.configureForTest("tools", "invokes_custom_tool");

        TrackingExecutor trackingExecutor = new TrackingExecutor(ForkJoinPool.commonPool());

        var parameters = new HashMap<String, Object>();
        var properties = new HashMap<String, Object>();
        var inputProp = new HashMap<String, Object>();
        inputProp.put("type", "string");
        inputProp.put("description", "String to encrypt");
        properties.put("input", inputProp);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("input"));

        ToolDefinition encryptTool = ToolDefinition.create("encrypt_string", "Encrypts a string", parameters,
                (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    String input = (String) args.get("input");
                    return CompletableFuture.completedFuture(input.toUpperCase());
                });

        CopilotClient client = new CopilotClient(createOptionsWithExecutor(trackingExecutor));
        client.createSession(new SessionConfig().setTools(List.of(encryptTool))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

        int beforeStop = trackingExecutor.getTaskCount();

        // stop() should use the provided executor for async session closure
        client.stop().get(30, TimeUnit.SECONDS);

        assertTrue(trackingExecutor.getTaskCount() > beforeStop,
                "Expected the tracking executor to have been invoked during client stop, "
                        + "but task count did not increase. CopilotClient.stop() is not "
                        + "routing session closure runAsync through the provided executor.");
    }
}

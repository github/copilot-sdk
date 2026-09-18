/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.SessionErrorEvent;
import com.github.copilot.generated.SessionIdleEvent;
import com.github.copilot.generated.rpc.SessionSendMessagesParams;
import com.github.copilot.rpc.AgentStopHookOutput;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.ToolDefinition;

class StructuredOutputE2ETest {
    @CopilotResponse
    public record Inventory(int count, String color) {
    }
    @CopilotResponse
    public record Answer(int answer) {
    }
    @CopilotResponse
    public record First(int first) {
    }
    @CopilotResponse
    public record Second(int second) {
    }

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null)
            ctx.close();
    }

    private SessionConfig config() {
        return new SessionConfig().setModel("gpt-4.1").setAvailableTools(List.of())
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setProvider(
                        new ProviderConfig().setType("openai").setWireApi("completions").setBaseUrl(ctx.getProxyUrl())
                                .setModelId("gpt-4.1").setWireModel("gpt-4.1").setApiKey("fake-token-for-e2e-tests")
                                .setHeaders(Map.of("Copilot-Integration-Id", "copilot-developer-cli",
                                        "Copilot-Harness-Id", "copilot-sdk", "X-GitHub-Api-Version", "2026-08-01")));
    }

    @Test
    void infersTypedResultAfterCustomTool() throws Exception {
        ctx.configureForTest("structured_output", "infers_typed_result_after_custom_tool");
        var calls = new AtomicInteger();
        var tool = ToolDefinition.from("get_inventory", "Get the current widget inventory.", () -> {
            calls.incrementAndGet();
            return "The inventory contains 42 red widgets.";
        });
        try (var client = ctx.createClient();
                var session = client.createSession(config().setTools(List.of(tool))).get()) {
            var result = session
                    .sendAndWait("Call get_inventory, then report the widget count and color.", Inventory.class)
                    .get(30, TimeUnit.SECONDS);
            assertEquals(new Inventory(42, "red"), result);
            assertTrue(calls.get() > 0);
            var ordinary = session
                    .sendAndWait(
                            new MessageOptions().setPrompt("Now reply with exactly the plain text HELLO, not JSON."))
                    .get(30, TimeUnit.SECONDS);
            assertEquals("HELLO", ordinary.getData().content().trim());
        }
    }

    @Test
    void sendsExplicitSchemaForMessageAndBatch() throws Exception {
        ctx.configureForTest("structured_output", "sends_explicit_schema_for_message_and_batch");
        try (var client = ctx.createClient(); var session = client.createSession(config()).get()) {
            var idle = new CompletableFuture<Void>();
            var replies = new CopyOnWriteArrayList<AssistantMessageEvent>();
            try (var subscription = session.on(event -> {
                if (event.getAgentId() != null)
                    return;
                if (event instanceof AssistantMessageEvent message)
                    replies.add(message);
                else if (event instanceof SessionIdleEvent)
                    idle.complete(null);
                else if (event instanceof SessionErrorEvent error)
                    idle.completeExceptionally(new IllegalStateException(error.getData().message()));
            })) {
                var params = JsonRpcClient.getObjectMapper()
                        .convertValue(
                                Map.of("messages",
                                        List.of(Map.of("prompt", "There are 42 red widgets in stock."),
                                                Map.of("prompt", "Report the widget count and color.")),
                                        "responseFormat",
                                        Map.of("type", "json_schema", "jsonSchema",
                                                Map.of("name", "inventory", "strict", true, "schema",
                                                        ResponseSchemas.forType(Inventory.class)))),
                                SessionSendMessagesParams.class);
                var accepted = session.getRpc().sendMessages(params).get(30, TimeUnit.SECONDS);
                idle.get(30, TimeUnit.SECONDS);
                var finalMessage = replies.stream().filter(
                        message -> accepted.messageIds().get(1).equals(message.getData().originatingMessageId()))
                        .reduce((first, second) -> second).orElseThrow();
                assertEquals(new Inventory(42, "red"),
                        JsonRpcClient.getObjectMapper().readValue(finalMessage.getData().content(), Inventory.class));
            }
            var raw = session.sendAndWait(new MessageOptions()
                    .setPrompt("The inventory now has 21 blue widgets. Report the new count and color.")
                    .setResponseSchema(ResponseSchemas.forType(Inventory.class))).get(30, TimeUnit.SECONDS);
            assertEquals(new Inventory(21, "blue"),
                    JsonRpcClient.getObjectMapper().readValue(raw.getData().content(), Inventory.class));
        }
    }

    @Test
    void typedWaitReturnsStopHookCorrectionAfterTerminalTool() throws Exception {
        ctx.configureForTest("structured_output", "typed_wait_returns_stop_hook_correction_after_terminal_tool");
        var calls = new AtomicInteger();
        var stops = new AtomicInteger();
        var tool = ToolDefinition.from("lookup_number", "Return the number needed for the calculation.", () -> {
            calls.incrementAndGet();
            return 58;
        }).isTerminal(true).skipPermission(true);
        var hooks = new SessionHooks().setOnAgentStop((input,
                invocation) -> CompletableFuture.completedFuture(stops.incrementAndGet() == 1
                        ? new AgentStopHookOutput().setDecision("block")
                                .setReason("Correct the answer to 99, not 63. Do not use tools.")
                        : null));
        try (var client = ctx.createClient();
                var session = client.createSession(config().setTools(List.of(tool)).setHooks(hooks)).get()) {
            var result = session.sendAndWait(
                    "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.",
                    Answer.class).get(30, TimeUnit.SECONDS);
            assertEquals(99, result.answer());
            assertEquals(1, calls.get());
            assertEquals(2, stops.get());
        }
    }

    @Test
    void typedWaitReturnsLateSteeringResponse() throws Exception {
        ctx.configureForTest("structured_output", "typed_wait_returns_late_steering_response");
        var stops = new AtomicInteger();
        var current = new java.util.concurrent.atomic.AtomicReference<CopilotSession>();
        var hooks = new SessionHooks().setOnAgentStop((input, invocation) -> {
            if (stops.incrementAndGet() == 1) {
                return current.get().send(new MessageOptions().setPrompt("Change the answer to 99. Do not use tools.")
                        .setMode("immediate")).thenApply(id -> null);
            }
            return CompletableFuture.completedFuture(null);
        });
        try (var client = ctx.createClient(); var session = client.createSession(config().setHooks(hooks)).get()) {
            current.set(session);
            assertEquals(99, session.sendAndWait("What is 19 + 23? Do not use tools.", Answer.class)
                    .get(30, TimeUnit.SECONDS).answer());
            assertEquals(2, stops.get());
        }
    }

    @Test
    void concurrentTypedSendsReturnTheirOwnResults() throws Exception {
        ctx.configureForTest("structured_output", "concurrent_typed_sends_return_their_own_results");
        var entered = new CompletableFuture<Void>();
        var release = new CompletableFuture<Object>();
        var tool = ToolDefinition.create("first_number", "Get the number for the first question.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()), invocation -> {
                    entered.complete(null);
                    return release;
                });
        try (var client = ctx.createClient();
                var session = client.createSession(config().setTools(List.of(tool))).get()) {
            var first = session.sendAndWait("Call first_number exactly once and report its returned number.",
                    First.class);
            try {
                CompletableFuture.anyOf(entered, first).get(30, TimeUnit.SECONDS);
                assertTrue(entered.isDone(), "First run must enter its tool");
                var second = session.sendAndWait("What is 30 + 7? Do not use tools.", Second.class);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                while (session.getRpc().queue.pendingItems().get(30, TimeUnit.SECONDS).items().isEmpty()) {
                    assertTrue(System.nanoTime() < deadline, "Second run was not queued");
                    Thread.sleep(10);
                }
                release.complete(42);
                assertEquals(42, first.get(30, TimeUnit.SECONDS).first());
                assertEquals(37, second.get(30, TimeUnit.SECONDS).second());
            } finally {
                release.complete(42);
            }
        }
    }
}

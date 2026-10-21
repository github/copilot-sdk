/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;

@AllowCopilotExperimental
class SetToolsIT {

    private static final String FRUIT_PROMPT = "Use lookup_fruit to find the fruit for code 42.";
    private static final String FRUIT_AND_VEGETABLE_PROMPT = "Use lookup_fruit to find the fruit for code 42 again, and use lookup_vegetable to find the vegetable for code 7.";
    private static final String VEGETABLE_PROMPT = "Use lookup_vegetable to find the vegetable for code 7.";

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

    @Test
    void replacesToolsOnACreatedSession() throws Exception {
        ctx.configureForTest("set_tools", "replaces_tools_on_a_created_session");

        var originalLookups = new CopyOnWriteArrayList<Integer>();
        var replacementLookups = new CopyOnWriteArrayList<Integer>();
        var vegetableLookups = new CopyOnWriteArrayList<Integer>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(
                    new SessionConfig().setTools(List.of(lookupFruit("apple", originalLookups), retiredLookup()))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);
            try {
                AssistantMessageEvent first = session.sendAndWait(new MessageOptions().setPrompt(FRUIT_PROMPT)).get(90,
                        TimeUnit.SECONDS);
                assertNotNull(first);
                assertTrue(first.getData().content().contains("apple"), first.getData().content());

                session.setTools(
                        List.of(lookupFruit("dragonfruit", replacementLookups), lookupVegetable(vegetableLookups)))
                        .get(30, TimeUnit.SECONDS);

                AssistantMessageEvent second = session
                        .sendAndWait(new MessageOptions().setPrompt(FRUIT_AND_VEGETABLE_PROMPT))
                        .get(90, TimeUnit.SECONDS);
                assertNotNull(second);
                assertTrue(second.getData().content().contains("dragonfruit"), second.getData().content());
                assertTrue(second.getData().content().contains("carrot"), second.getData().content());
                assertEquals(List.of(42), originalLookups);
                assertEquals(List.of(42), replacementLookups);
                assertEquals(List.of(7), vegetableLookups);

                // Model requests after the replacement offer exactly the new tool set.
                List<Map<String, Object>> exchanges = ctx.getExchanges();
                int replacedFrom = indexOfPrompt(exchanges, FRUIT_AND_VEGETABLE_PROMPT);
                assertTrue(replacedFrom > 0, "Expected model requests before and after the replacement");
                assertOffered(exchanges.subList(0, replacedFrom), List.of("lookup_fruit", "retired_lookup"),
                        List.of("lookup_vegetable"));
                assertOffered(exchanges.subList(replacedFrom, exchanges.size()),
                        List.of("lookup_fruit", "lookup_vegetable"), List.of("retired_lookup"));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void replacesToolsOnAResumedSession() throws Exception {
        ctx.configureForTest("set_tools", "replaces_tools_on_a_resumed_session");

        try (CopilotClient client = ctx.createClient()) {
            var createdLookups = new CopyOnWriteArrayList<Integer>();
            CopilotSession created = client
                    .createSession(new SessionConfig().setTools(List.of(lookupFruit("apple", createdLookups)))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);
            String sessionId = created.getSessionId();
            AssistantMessageEvent first = created.sendAndWait(new MessageOptions().setPrompt(FRUIT_PROMPT)).get(90,
                    TimeUnit.SECONDS);
            assertNotNull(first);
            assertTrue(first.getData().content().contains("apple"), first.getData().content());
            assertEquals(List.of(42), createdLookups);
            created.close();

            var fruitLookups = new CopyOnWriteArrayList<Integer>();
            var vegetableLookups = new CopyOnWriteArrayList<Integer>();
            CopilotSession resumed = client.resumeSession(sessionId,
                    new ResumeSessionConfig().setTools(List.of(lookupFruit("apple", fruitLookups)))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);
            try {
                resumed.setTools(List.of(lookupVegetable(vegetableLookups))).get(30, TimeUnit.SECONDS);

                AssistantMessageEvent answer = resumed.sendAndWait(new MessageOptions().setPrompt(VEGETABLE_PROMPT))
                        .get(90, TimeUnit.SECONDS);
                assertNotNull(answer);
                assertTrue(answer.getData().content().contains("carrot"), answer.getData().content());
                assertEquals(List.of(7), vegetableLookups);
                assertEquals(List.of(), fruitLookups);

                List<Map<String, Object>> exchanges = ctx.getExchanges();
                int replacedFrom = indexOfPrompt(exchanges, VEGETABLE_PROMPT);
                assertTrue(replacedFrom > 0, "Expected model requests before and after the replacement");
                assertOffered(exchanges.subList(replacedFrom, exchanges.size()), List.of("lookup_vegetable"),
                        List.of("lookup_fruit"));
            } finally {
                resumed.close();
            }
        }
    }

    @Test
    void keepsThePreviousToolsWhenAReplacementIsRejected() throws Exception {
        ctx.configureForTest("set_tools", "keeps_the_previous_tools_when_a_replacement_is_rejected");

        var originalLookups = new CopyOnWriteArrayList<Integer>();
        var replacementLookups = new CopyOnWriteArrayList<Integer>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setTools(List.of(lookupFruit("apple", originalLookups)))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);
            try {
                assertThrows(ExecutionException.class,
                        () -> session.setTools(List.of(lookupFruit("dragonfruit", replacementLookups), invalidTool()))
                                .get(30, TimeUnit.SECONDS));

                AssistantMessageEvent answer = session.sendAndWait(new MessageOptions().setPrompt(FRUIT_PROMPT)).get(90,
                        TimeUnit.SECONDS);
                assertNotNull(answer);
                assertTrue(answer.getData().content().contains("apple"), answer.getData().content());
                assertEquals(List.of(42), originalLookups);
                assertEquals(List.of(), replacementLookups);
            } finally {
                session.close();
            }
        }
    }

    private static ToolDefinition lookupFruit(String fruit, List<Integer> calls) {
        return ToolDefinition.create("lookup_fruit", "Looks up the fruit for a numeric code", codeSchema("Fruit code"),
                invocation -> {
                    calls.add(((Number) invocation.getArguments().get("code")).intValue());
                    return CompletableFuture.completedFuture(fruit);
                });
    }

    private static ToolDefinition lookupVegetable(List<Integer> calls) {
        return ToolDefinition.create("lookup_vegetable", "Looks up the vegetable for a numeric code",
                codeSchema("Vegetable code"), invocation -> {
                    calls.add(((Number) invocation.getArguments().get("code")).intValue());
                    return CompletableFuture.completedFuture("carrot");
                });
    }

    private static ToolDefinition retiredLookup() {
        return ToolDefinition.create("retired_lookup", "Looks up a retired value",
                Map.of("type", "object", "properties", Map.of()),
                invocation -> CompletableFuture.completedFuture("retired"));
    }

    private static ToolDefinition invalidTool() {
        return ToolDefinition.create("invalid.tool", "Has a name the runtime rejects",
                Map.of("type", "object", "properties", Map.of()),
                invocation -> CompletableFuture.completedFuture("never"));
    }

    private static Map<String, Object> codeSchema(String description) {
        return Map.of("type", "object", "properties",
                Map.of("code", Map.of("type", "integer", "description", description)), "required", List.of("code"));
    }

    /**
     * Returns the index of the first model request that carries {@code prompt} as a
     * user message, or -1.
     */
    private static int indexOfPrompt(List<Map<String, Object>> exchanges, String prompt) {
        for (int i = 0; i < exchanges.size(); i++) {
            if (exchanges.get(i).get("request") instanceof Map<?, ?> request
                    && request.get("messages") instanceof List<?> messages) {
                for (Object message : messages) {
                    if (message instanceof Map<?, ?> fields && "user".equals(fields.get("role"))
                            && String.valueOf(fields.get("content")).contains(prompt)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Asserts that every model request offered the tools in {@code offered} and
     * none of the tools in {@code notOffered}.
     */
    private static void assertOffered(List<Map<String, Object>> exchanges, List<String> offered,
            List<String> notOffered) {
        for (Map<String, Object> exchange : exchanges) {
            var tools = new ArrayList<String>();
            if (exchange.get("request") instanceof Map<?, ?> request && request.get("tools") instanceof List<?> list) {
                for (Object tool : list) {
                    if (tool instanceof Map<?, ?> fields && fields.get("function") instanceof Map<?, ?> function) {
                        tools.add(String.valueOf(function.get("name")));
                    }
                }
            }
            assertTrue(tools.containsAll(offered), "Offered " + tools + ", expected " + offered);
            for (String name : notOffered) {
                assertFalse(tools.contains(name), "Offered " + tools + ", expected no " + name);
            }
        }
    }
}

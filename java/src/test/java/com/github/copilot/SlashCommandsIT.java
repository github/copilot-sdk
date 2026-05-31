/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.SessionCommandsListResult;
import com.github.copilot.generated.rpc.SessionCommandsInvokeParams;
import com.github.copilot.generated.rpc.SlashCommandAgentPromptResult;
import com.github.copilot.generated.rpc.SlashCommandCompletedResult;
import com.github.copilot.generated.rpc.SlashCommandInfo;
import com.github.copilot.generated.rpc.SlashCommandInvocationResult;
import com.github.copilot.generated.rpc.SlashCommandSelectSubcommandResult;
import com.github.copilot.generated.rpc.SlashCommandTextResult;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Failsafe integration test that exercises slash commands against the live
 * Copilot CLI (not the replay proxy).
 * <p>
 * Requires the CLI to be installed and the user to be signed in. Uses
 * {@link TestUtil#findCliPath()} so the test harness binary is found in CI.
 */
class SlashCommandsIT {

    private static CopilotClient client;
    private static CopilotSession session;

    @BeforeAll
    static void setup() throws Exception {
        String cliPath = TestUtil.findCliPath();
        CopilotClientOptions options = new CopilotClientOptions().setCliPath(cliPath).setUseLoggedInUser(true);
        client = new CopilotClient(options);
        client.start().get(30, TimeUnit.SECONDS);
        session = client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                .get(30, TimeUnit.SECONDS);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (session != null) {
            session.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void listCommandsReturnsAtLeast20() throws Exception {
        SessionCommandsListResult result = session.getRpc().commands.list().get(15, TimeUnit.SECONDS);

        assertNotNull(result, "commands.list result must not be null");
        assertNotNull(result.commands(), "commands list must not be null");
        assertTrue(result.commands().size() >= 20, "Expected at least 20 commands but got " + result.commands().size());

        Pattern namePattern = Pattern.compile("^[a-z].*$");

        // Print every command so we can pick one for the next iteration
        System.out.println("=== Available slash commands ===");
        for (SlashCommandInfo cmd : result.commands()) {
            System.out.printf("  /%s  kind=%s  desc=%s  aliases=%s%n", cmd.name(), cmd.kind(), cmd.description(),
                    cmd.aliases());
            assertTrue(namePattern.matcher(cmd.name()).matches(),
                    "Command name should match /^[a-z].*$/ but was: " + cmd.name());
        }
        System.out.println("=== Total: " + result.commands().size() + " commands ===");
    }

    @Test
    void autoPilotToggle() throws Exception {
        SlashCommandInvocationResult first = session.getRpc().commands
                .invoke(new SessionCommandsInvokeParams(null, "autopilot", null)).get(15, TimeUnit.SECONDS);
        SlashCommandInvocationResult second = session.getRpc().commands
                .invoke(new SessionCommandsInvokeParams(null, "autopilot", null)).get(15, TimeUnit.SECONDS);

        String firstOutput = extractDisplayText(first);
        String secondOutput = extractDisplayText(second);

        assertTrue(!firstOutput.isBlank(), "First /autopilot invocation should return non-empty output");
        assertTrue(!secondOutput.isBlank(), "Second /autopilot invocation should return non-empty output");
        assertNotEquals(firstOutput, secondOutput,
                "Two consecutive /autopilot invocations should produce different output because mode toggles");

        List<String> firstTokens = tokenizeForComparison(firstOutput);
        List<String> secondTokens = tokenizeForComparison(secondOutput);
        assertTrue(!firstTokens.isEmpty(), "First /autopilot output should include at least one token");
        assertTrue(!secondTokens.isEmpty(), "Second /autopilot output should include at least one token");

        List<String> commonInOrder = commonTokensInOrder(firstTokens, secondTokens);
        assertTrue(!commonInOrder.isEmpty(),
                "Outputs should share at least one token in the same order to indicate similar structure");

        Set<String> firstOnly = new HashSet<>(firstTokens);
        firstOnly.removeAll(new HashSet<>(secondTokens));
        Set<String> secondOnly = new HashSet<>(secondTokens);
        secondOnly.removeAll(new HashSet<>(firstTokens));
        assertTrue(!firstOnly.isEmpty() || !secondOnly.isEmpty(),
                "Outputs should differ by at least one token to reflect the toggle change");

        System.out.println("First /autopilot result: " + firstOutput);
        System.out.println("Second /autopilot result: " + secondOutput);
    }

    private static String extractDisplayText(SlashCommandInvocationResult result) {
        assertNotNull(result, "slash command result must not be null");

        if (result instanceof SlashCommandTextResult textResult) {
            return valueOrEmpty(textResult.getText());
        }
        if (result instanceof SlashCommandCompletedResult completedResult) {
            return valueOrEmpty(completedResult.getMessage());
        }
        if (result instanceof SlashCommandAgentPromptResult promptResult) {
            String display = valueOrEmpty(promptResult.getDisplayPrompt());
            if (!display.isBlank()) {
                return display;
            }
            return valueOrEmpty(promptResult.getPrompt());
        }
        if (result instanceof SlashCommandSelectSubcommandResult selectResult) {
            String title = valueOrEmpty(selectResult.getTitle());
            if (!title.isBlank()) {
                return title;
            }
            return valueOrEmpty(selectResult.getCommand());
        }

        return valueOrEmpty(result.getKind());
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> tokenizeForComparison(String text) {
        List<String> tokens = new ArrayList<>();
        Pattern wordPattern = Pattern.compile("[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
        var matcher = wordPattern.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static List<String> commonTokensInOrder(List<String> first, List<String> second) {
        List<String> common = new ArrayList<>();
        int secondIndex = 0;

        for (String token : first) {
            while (secondIndex < second.size()) {
                String candidate = second.get(secondIndex++);
                if (token.equals(candidate)) {
                    common.add(token);
                    break;
                }
            }
            if (secondIndex >= second.size()) {
                break;
            }
        }

        return common;
    }
}

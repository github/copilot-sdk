/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PostToolUseHookOutput;
import com.github.copilot.rpc.PreToolUseHookOutput;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

/** Tests for SDK callback hook behavior with the native runtime. */
public class HooksTest {

    private static final String UNSUPPORTED_SDK_HOOKS_MESSAGE = "SDK hook callbacks are no longer supported";

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
    void testRejectsSdkCallbackHooks() throws Exception {
        ctx.initializeProxy();

        var hookCases = List.of(
                new SessionHooks().setOnPreToolUse(
                        (input, invocation) -> CompletableFuture.completedFuture(PreToolUseHookOutput.allow())),
                new SessionHooks().setOnPostToolUse(
                        (input, invocation) -> CompletableFuture.completedFuture((PostToolUseHookOutput) null)),
                new SessionHooks().setOnPreToolUse(
                        (input, invocation) -> CompletableFuture.completedFuture(PreToolUseHookOutput.deny())),
                new SessionHooks()
                        .setOnPreToolUse(
                                (input, invocation) -> CompletableFuture.completedFuture(PreToolUseHookOutput.allow()))
                        .setOnPostToolUse((input, invocation) -> CompletableFuture
                                .completedFuture((PostToolUseHookOutput) null)));

        try (CopilotClient client = ctx.createClient()) {
            for (SessionHooks hooks : hookCases) {
                var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setHooks(hooks);

                var ex = assertThrows(ExecutionException.class, () -> client.createSession(config).get());
                assertTrue(ex.toString().contains(UNSUPPORTED_SDK_HOOKS_MESSAGE),
                        () -> "Expected unsupported hooks error, got: " + ex);
            }
        }
    }
}

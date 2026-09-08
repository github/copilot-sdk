/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.GitHubTokenProviderResult;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class GitHubTokenProviderRegistryTest {

    @Test
    void staticTokenAndProviderAreMutuallyExclusive() {
        try (var client = new CopilotClient()) {
            var create = new SessionConfig().setGitHubToken("static")
                    .setGitHubTokenProvider(
                            args -> CompletableFuture.completedFuture(GitHubTokenProviderResult.cancelled()))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
            var createError = assertThrows(CompletionException.class, () -> client.createSession(create).join());
            assertInstanceOf(IllegalArgumentException.class, createError.getCause());

            var resume = new ResumeSessionConfig().setGitHubToken("static")
                    .setGitHubTokenProvider(
                            args -> CompletableFuture.completedFuture(GitHubTokenProviderResult.cancelled()))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
            var resumeError = assertThrows(CompletionException.class,
                    () -> client.resumeSession("session", resume).join());
            assertInstanceOf(IllegalArgumentException.class, resumeError.getCause());
        }
    }

    @Test
    void registrationRollbackAndResumeReplacementAreIsolated() {
        var registry = new GitHubTokenProviderRegistry();
        var first = registry.register(args -> CompletableFuture.completedFuture(GitHubTokenProviderResult.cancelled()));
        var second = registry
                .register(args -> CompletableFuture.completedFuture(GitHubTokenProviderResult.cancelled()));

        assertNotNull(registry.get(first.id()));
        assertNotNull(registry.get(second.id()));
        first.claim("session-1");
        second.claim("session-1");
        assertNull(registry.get(first.id()));
        assertNotNull(registry.get(second.id()));

        first.close();
        assertNotNull(registry.get(second.id()));
        second.close();
        assertNull(registry.get(second.id()));

        var retired = registry
                .register(args -> CompletableFuture.completedFuture(GitHubTokenProviderResult.cancelled()));
        retired.claim("session-2");
        registry.retire("session-2");
        assertNull(registry.get(retired.id()));

        var failed = registry
                .register(args -> CompletableFuture.completedFuture(GitHubTokenProviderResult.cancelled()));
        failed.close();
        assertNull(registry.get(failed.id()));
    }
}

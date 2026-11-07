/* Copyright (c) Microsoft Corporation. All rights reserved. */
package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import com.github.copilot.generated.rpc.HostLocalServerOptions;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.RuntimeConnection;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AllowCopilotExperimental
@Timeout(120)
@EnabledIfEnvironmentVariable(named = "COPILOT_RUNTIME_HOST_E2E", matches = "1")
class HostingAuthenticationIT {
    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get();
    }

    private static AhpTestClient driver(E2ETestContext context) throws Exception {
        return new AhpTestClient(context.getRepoRoot(), "../test/harness/hostAuthenticationDriver.mjs");
    }

    private static CopilotClient client(E2ETestContext context) {
        var environment = context.getEnvironment();
        for (String key : List.of("GH_TOKEN", "GITHUB_TOKEN", "COPILOT_GITHUB_TOKEN", "COPILOT_HMAC_KEY",
                "CAPI_HMAC_KEY", "COPILOT_SDK_AUTH_TOKEN")) {
            environment.put(key, "");
        }
        // Clearing ambient credential aliases requires an isolated child environment.
        return context.createClient(new CopilotClientOptions().setConnection(RuntimeConnection.forStdio())
                .setEnvironment(environment).setGitHubToken("").setUseLoggedInUser(false));
    }

    private static AhpHostOptions options(AhpHostAuthentication policy) {
        return new AhpHostOptions().setAuthentication(policy).setLocalServer(HostLocalServerOptions.builder()
                .hostname(null).port(null).token("independent-websocket-secret").requireConnectionToken(null).build());
    }

    private static AhpHostAuthentication custom(
            java.util.function.Function<AhpAuthenticationRequest, CompletableFuture<AhpAuthenticationResult>> validator) {
        return AhpHostAuthentication.custom("application-owner", validator);
    }

    private static AhpAuthenticationResult accepted() {
        return new AhpAuthenticationResult("application-owner", Instant.now().plusSeconds(60).toString());
    }

    private static String connect(AhpTestClient ahp, AhpHost host, String mode, String token) throws Exception {
        var command = new HashMap<String, Object>(
                Map.of("op", "connect", "url", host.getUrl(), "token", host.getToken(), "authentication", mode));
        if (token != null) {
            command.put("customToken", token);
        }
        return ahp.request(command).path("clientId").asText();
    }

    private static ProviderConfig provider(String endpoint) {
        return new ProviderConfig().setType("openai").setWireApi("completions").setBaseUrl(endpoint)
                .setApiKey("inference-only-key");
    }

    @ParameterizedTest
    @ValueSource(strings = {"custom", "none"})
    void hostsByokCreateAndColdResumeWithTrustedIdentity(String mode) throws Exception {
        try (var context = E2ETestContext.create(); var ahp = driver(context)) {
            String endpoint = ahp.request(Map.of("op", "endpoint", "responseText", "JAVA_HOST_BYOK_OK")).path("url")
                    .asText();
            var policy = "custom".equals(mode) ? custom(request -> {
                assertEquals("admission-only-key", request.token());
                return CompletableFuture.completedFuture(accepted());
            }) : AhpHostAuthentication.none();
            var created = new CompletableFuture<CopilotSession>();
            var resumed = new CompletableFuture<CopilotSession>();
            String sessionId;
            String originalId;
            try (var owner = client(context);
                    var host = await(owner.startAhpHost(
                            options(policy).setAllowCreatingNewSessions(true).setCreateSession(request -> {
                                assertEquals(
                                        new AhpSessionAuthentication(mode,
                                                "custom".equals(mode) ? "application-owner" : null),
                                        request.authentication());
                                assertNull(request.config().getGitHubToken());
                                return owner
                                        .createSession(
                                                request.config().setModel("local-model").setProvider(provider(endpoint))
                                                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                                        .thenApply(session -> {
                                            created.complete(session);
                                            return session;
                                        });
                            }).setWorkspace(context.getWorkDir().toString())))) {
                String connection = connect(ahp, host, mode, "custom".equals(mode) ? "admission-only-key" : null);
                assertThrows(IOException.class, () -> ahp.request(Map.of("op", "connect", "url", host.getUrl(), "token",
                        "wrong-websocket-secret", "authentication", mode)));
                sessionId = ahp.request(
                        Map.of("op", "create", "clientId", connection, "workDir", context.getWorkDir().toString()))
                        .path("sessionId").asText();
                var original = await(created);
                originalId = original.getSessionId();
                assertEquals("JAVA_HOST_BYOK_OK", ahp.request(
                        Map.of("op", "turn", "clientId", connection, "sessionId", sessionId, "prompt", "Say hello"))
                        .path("text").asText());
                await(host.dispose());
                ahp.request(Map.of("op", "close", "clientId", connection));
                original.close();
            }
            try (var owner = client(context);
                    var host = await(owner.startAhpHost(options(policy).setResumeSession(request -> {
                        assertEquals(originalId, request.sessionId());
                        assertEquals(
                                new AhpSessionAuthentication(mode, "custom".equals(mode) ? "application-owner" : null),
                                request.authentication());
                        assertNull(request.config().getGitHubToken());
                        return owner
                                .resumeSession(request.sessionId(),
                                        request.config().setModel("local-model").setProvider(provider(endpoint))
                                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                                .thenApply(session -> {
                                    resumed.complete(session);
                                    return session;
                                });
                    }).setWorkspace(context.getWorkDir().toString())))) {
                String connection = connect(ahp, host, mode, "custom".equals(mode) ? "admission-only-key" : null);
                ahp.request(Map.of("op", "attach", "clientId", connection, "sessionId", sessionId));
                assertEquals(originalId, await(resumed).getSessionId());
                assertEquals("JAVA_HOST_BYOK_OK", ahp.request(Map.of("op", "turn", "clientId", connection, "sessionId",
                        sessionId, "prompt", "Say hello again")).path("text").asText());
                await(host.dispose());
                ahp.request(Map.of("op", "close", "clientId", connection));
                resumed.join().close();
            }
            var inference = ahp.request(Map.of("op", "endpoint")).path("requests");
            assertEquals(2, inference.size());
            for (var request : inference) {
                assertEquals("Bearer inference-only-key", request.path("authorization").asText());
                assertFalse(request.toString().contains("admission-only-key"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "throw", "identity", "invalid", "past", "missing"})
    void rejectsInvalidCredentialsBeforeMaterialization(String failure) throws Exception {
        try (var context = E2ETestContext.create(); var ahp = driver(context); var owner = client(context)) {
            var creates = new AtomicInteger();
            try (var host = await(owner.startAhpHost(options(custom(request -> {
                assertEquals("decrypted-admission", request.token());
                if ("throw".equals(failure)) {
                    throw new IllegalStateException("validator unavailable");
                }
                return CompletableFuture.completedFuture("null".equals(failure)
                        ? null
                        : new AhpAuthenticationResult(
                                "identity".equals(failure) ? "another-owner" : "application-owner",
                                "missing".equals(failure)
                                        ? null
                                        : "invalid".equals(failure)
                                                ? "not-a-date"
                                                : "past".equals(failure)
                                                        ? "2000-01-01T00:00:00Z"
                                                        : accepted().expiresAt()));
            })).setCreateSession(request -> {
                creates.incrementAndGet();
                return CompletableFuture.failedFuture(new IllegalStateException("must not materialize"));
            })))) {
                String connection = connect(ahp, host, "custom", null);
                assertThrows(IOException.class, () -> ahp.request(
                        Map.of("op", "authenticate", "clientId", connection, "customToken", "decrypted-admission")));
                assertThrows(IOException.class, () -> ahp.request(
                        Map.of("op", "create", "clientId", connection, "workDir", context.getWorkDir().toString())));
                assertEquals(0, creates.get());
            }
        }
    }

    @Test
    void cancellationCompletesAndLateValidationCannotAdmit() throws Exception {
        try (var context = E2ETestContext.create(); var ahp = driver(context); var owner = client(context)) {
            var entered = new CompletableFuture<AhpAuthenticationRequest>();
            var validation = new CompletableFuture<AhpAuthenticationResult>();
            try (var host = await(owner.startAhpHost(options(custom(request -> {
                entered.complete(request);
                return validation;
            }))))) {
                var pending = CompletableFuture.supplyAsync(() -> {
                    try {
                        return connect(ahp, host, "custom", "pending-token");
                    } catch (Exception error) {
                        throw new java.util.concurrent.CompletionException(error);
                    }
                });
                var request = await(entered);
                await(host.dispose());
                await(request.cancellation().toCompletableFuture());
                assertThrows(ExecutionException.class, () -> await(pending));
                validation.complete(accepted());
                assertTrue(pending.isCompletedExceptionally());
            }
        }
    }

    @Test
    void expirationRequiresReauthenticationForTheConfiguredOwner() throws Exception {
        try (var context = E2ETestContext.create(); var ahp = driver(context); var owner = client(context)) {
            record Credential(String token, Instant expiry, AhpAuthenticationResult result) {
            }
            var calls = new AtomicInteger();
            var issued = new AtomicReference<Credential>();
            var expiredAdmissionErrors = java.util.Set.of(
                    "RPC error -32603: internal error: application authentication failed",
                    "RPC error -32007: authentication required for https://copilot.github.com/hosting/authentication");
            try (var host = await(owner.startAhpHost(options(custom(request -> {
                assertTrue("short-lived".equals(request.token()) || "renewed".equals(request.token()),
                        "Unexpected fixture token: " + request.token());
                var expiry = Instant.now().plusSeconds("renewed".equals(request.token()) ? 60 : 2);
                var result = new AhpAuthenticationResult("application-owner", expiry.toString());
                var reply = CompletableFuture.completedFuture(result);
                issued.set(new Credential(request.token(), expiry, result));
                calls.incrementAndGet();
                return reply;
            }))))) {
                String connection = connect(ahp, host, "custom", null);
                int attempts = 0;
                for (;;) {
                    issued.set(null);
                    int before = calls.get();
                    attempts++;
                    IOException rejected = null;
                    try {
                        ahp.request(Map.of("op", "authenticate", "clientId", connection, "customToken", "short-lived"));
                    } catch (IOException failure) {
                        if (!expiredAdmissionErrors.contains(failure.getMessage())) {
                            throw failure;
                        }
                        rejected = failure;
                    }
                    var credential = issued.get();
                    assertEquals(before + 1, calls.get(), "Each authentication must invoke the validator once");
                    assertNotNull(credential, "The validator must return this attempt's fixture credential");
                    assertEquals("short-lived", credential.token());
                    assertEquals("application-owner", credential.result().identity());
                    assertEquals(credential.expiry().toString(), credential.result().expiresAt());
                    if (rejected == null) {
                        break;
                    }
                    if (credential.expiry().isAfter(Instant.now())) {
                        throw rejected;
                    }
                    java.util.logging.Logger.getLogger(HostingAuthenticationIT.class.getName()).warning(
                            "Retrying initial authentication with an already-expired fixture credential: " + rejected);
                }
                ahp.request(Map.of("op", "expired", "clientId", connection));
                assertThrows(IOException.class, () -> ahp.request(
                        Map.of("op", "create", "clientId", connection, "workDir", context.getWorkDir().toString())));
                issued.set(null);
                int beforeRenewal = calls.get();
                ahp.request(Map.of("op", "authenticate", "clientId", connection, "customToken", "renewed"));
                assertEquals(beforeRenewal + 1, calls.get(), "Renewal must invoke the validator once");
                assertEquals("renewed", issued.get().token());
                assertEquals(attempts + 1, calls.get());
            }
        }
    }
}

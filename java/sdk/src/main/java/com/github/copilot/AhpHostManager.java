/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.rpc.HostExitReason;
import com.github.copilot.generated.rpc.HostExitedNotification;
import com.github.copilot.generated.rpc.HostStartRequest;
import com.github.copilot.generated.rpc.ServerHostApi;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllowCopilotExperimental
final class AhpHostManager {
    private static final Logger LOG = Logger.getLogger(AhpHostManager.class.getName());
    // Optional getters are excluded from config serialization, not from incoming
    // host settings.
    private static final ObjectMapper CONFIG_MAPPER = JsonRpcClient.getObjectMapper().copy()
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public boolean hasIgnoreMarker(AnnotatedMember member) {
                    if ((member.getDeclaringClass() == SessionConfig.class
                            || member.getDeclaringClass() == ResumeSessionConfig.class)
                            && member.getRawType() == Optional.class) {
                        return false;
                    }
                    return super.hasIgnoreMarker(member);
                }
            });
    private final Object gate = new Object();
    private final Map<String, Host> hosts = new HashMap<>();
    private final Map<String, CopilotSession> sessions;
    private final Executor executor;

    private static final class Host {
        final String id = UUID.randomUUID().toString();
        final AhpHostOptions options;
        final ServerHostApi rpc;
        final Map<String, Handoff> handoffs = new HashMap<>();
        final Map<String, Authentication> authentications = new HashMap<>();
        boolean exited;

        Host(ServerHostApi rpc, AhpHostOptions options) {
            this.rpc = rpc;
            this.options = new AhpHostOptions(options);
        }
    }

    private static final class Authentication {
        final CompletableFuture<Void> cancellation = new CompletableFuture<>();
        final CompletableFuture<AhpAuthenticationResult> response = new CompletableFuture<>();
    }

    static void validateOptions(AhpHostOptions options) {
        var policy = options == null ? null : options.getAuthentication();
        if (policy == null
                || !java.util.Set.of("github", "none", "custom").contains(policy.mode() == null ? "" : policy.mode())) {
            throw new IllegalArgumentException("An explicit authentication mode is required");
        }
        if ("custom".equals(policy.mode()) && (policy.requireIdentity() == null || policy.requireIdentity().isBlank()
                || policy.authenticate() == null)) {
            throw new IllegalArgumentException("Custom authentication requires requireIdentity and authenticate");
        }
        if (!"github".equals(policy.mode()) && options.getGithubEnvironment() != null) {
            throw new IllegalArgumentException("githubEnvironment requires github authentication");
        }
    }

    private static final class Handoff {
        final Host host;
        final String id;
        final String sessionId;
        final ObjectNode expected;
        final CopilotSession retained;
        final CompletableFuture<Void> cancellation = new CompletableFuture<>();
        final CompletableFuture<Map<String, String>> response = new CompletableFuture<>();
        Map<CopilotSession, JsonNode> configs = new IdentityHashMap<>();
        CopilotSession session;
        boolean released;

        Handoff(Host host, String id, String sessionId, ObjectNode expected, CopilotSession retained) {
            this.host = host;
            this.id = id;
            this.sessionId = sessionId;
            this.expected = expected;
            this.retained = retained;
        }
    }

    AhpHostManager(Map<String, CopilotSession> sessions, Executor executor) {
        this.sessions = sessions;
        this.executor = executor;
    }

    CompletableFuture<AhpHost> start(ServerHostApi rpc, AhpHostOptions options) {
        var host = new Host(rpc, options);
        synchronized (gate) {
            hosts.put(host.id, host);
        }
        var snapshot = host.options;
        var policy = snapshot.getAuthentication();
        return rpc
                .start(new HostStartRequest(host.id).setComputeId(snapshot.getComputeId())
                        .setWorkspace(snapshot.getWorkspace())
                        .setAuthentication("custom".equals(policy.mode())
                                ? Map.of("mode", policy.mode(), "requireIdentity", policy.requireIdentity())
                                : Map.of("mode", policy.mode()))
                        .setAllowCreatingNewSessions(snapshot.getAllowCreatingNewSessions())
                        .setLocalServer(snapshot.getLocalServer()).setGitHubEnvironment(snapshot.getGithubEnvironment())
                        .setSessionFactory(snapshot.getCreateSession() != null)
                        .setResumeFactory(snapshot.getResumeSession() != null))
                .thenApply(info -> new AhpHost(info, rpc)).whenComplete((result, error) -> {
                    if (error != null) {
                        endHost(host, null);
                    }
                });
    }

    void register(JsonRpcClient rpc) {
        rpc.registerMethodHandler("host.validateAuthentication", (id, params) -> {
            CompletableFuture<AhpAuthenticationResult> response;
            try {
                response = authenticate(params);
            } catch (Exception error) {
                response = CompletableFuture.failedFuture(error);
            }
            response.whenComplete((result, error) -> {
                try {
                    if (error == null) {
                        rpc.sendResponse(Long.parseLong(id), result);
                    } else {
                        rpc.sendErrorResponse(Long.parseLong(id), -32603, unwrap(error).getMessage());
                    }
                } catch (IOException | NumberFormatException errorSending) {
                    LOG.log(Level.WARNING, "Could not send AHP authentication response", errorSending);
                }
            });
        });
        rpc.registerMethodHandler("host.authenticationCancelled", (id, params) -> {
            Authentication authentication;
            synchronized (gate) {
                Host host = hosts.get(text(params, "hostId"));
                authentication = host == null ? null : host.authentications.remove(text(params, "requestId"));
            }
            if (authentication != null) {
                cancelAuthentication(authentication);
            }
        });
        rpc.registerMethodHandler("host.materializeSession", (id, params) -> {
            CompletableFuture<Map<String, String>> response;
            try {
                response = materialize(params);
            } catch (Exception error) {
                response = CompletableFuture.failedFuture(error);
            }
            response.whenComplete((result, error) -> {
                try {
                    long requestId = Long.parseLong(id);
                    if (error == null) {
                        rpc.sendResponse(requestId, result);
                    } else {
                        rpc.sendErrorResponse(requestId, -32603, unwrap(error).getMessage());
                    }
                } catch (IOException | NumberFormatException errorSending) {
                    LOG.log(Level.WARNING, "Could not send AHP factory response", errorSending);
                }
            });
        });
        rpc.registerMethodHandler("host.sessionReleased", (id, params) -> {
            try {
                Handoff handoff;
                synchronized (gate) {
                    Host host = hosts.get(text(params, "hostId"));
                    handoff = host == null ? null : host.handoffs.get(text(params, "handoffId"));
                }
                if (handoff != null) {
                    release(handoff);
                }
            } catch (Exception error) {
                LOG.log(Level.WARNING, "Invalid AHP session release notification", error);
            }
        });
        rpc.registerMethodHandler("host.exited", (id, params) -> {
            try {
                var info = JsonRpcClient.getObjectMapper().treeToValue(params, HostExitedNotification.class);
                Host host;
                synchronized (gate) {
                    host = hosts.get(info.hostId());
                }
                if (host != null) {
                    endHost(host, new AhpHostExit(info.hostId(), info.reason(), info.exitCode(), info.error()));
                }
            } catch (Exception error) {
                LOG.log(Level.WARNING, "Invalid AHP host exit notification", error);
            }
        });
    }

    private CompletableFuture<AhpAuthenticationResult> authenticate(JsonNode params) {
        String requestId = text(params, "requestId");
        String token = text(params, "token");
        Host host;
        var entry = new Authentication();
        synchronized (gate) {
            host = hosts.get(text(params, "hostId"));
            if (host == null || host.exited || !"custom".equals(host.options.getAuthentication().mode())
                    || host.authentications.putIfAbsent(requestId, entry) != null) {
                throw new IllegalStateException("AHP authenticator unavailable or duplicate request");
            }
        }
        entry.response.whenComplete((result, error) -> {
            synchronized (gate) {
                host.authentications.remove(requestId, entry);
            }
            entry.cancellation.completeAsync(() -> null);
        });
        try {
            CompletableFuture.supplyAsync(() -> {
                synchronized (gate) {
                    if (host.exited || entry.response.isDone()) {
                        throw new CancellationException("AHP authentication ended");
                    }
                }
                return Objects
                        .requireNonNull(
                                host.options.getAuthentication().authenticate()
                                        .apply(new AhpAuthenticationRequest(token,
                                                entry.cancellation.minimalCompletionStage())),
                                "AHP authenticator returned a null future");
            }, executor).thenCompose(future -> future).whenComplete((result, error) -> {
                synchronized (gate) {
                    if (host.exited || entry.response.isDone()) {
                        return;
                    }
                    try {
                        if (error != null) {
                            throw new CompletionException(unwrap(error));
                        }
                        if (result == null
                                || !host.options.getAuthentication().requireIdentity().equals(result.identity())
                                || result.expiresAt() == null
                                || !result.expiresAt().matches(
                                        "\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([Zz]|[+-]\\d{2}:\\d{2})")
                                || !OffsetDateTime.parse(result.expiresAt()).toInstant().isAfter(Instant.now())) {
                            throw new IllegalArgumentException(
                                    "AHP authentication rejected: invalid identity or expiration");
                        }
                        entry.response.complete(result);
                    } catch (Exception invalid) {
                        entry.response.completeExceptionally(invalid);
                    }
                }
            });
        } catch (Exception error) {
            entry.response.completeExceptionally(error);
        }
        return entry.response;
    }

    private static void cancelAuthentication(Authentication entry) {
        entry.response.completeExceptionally(new CancellationException("AHP authentication ended"));
        entry.cancellation.completeAsync(() -> null);
    }

    void capture(CopilotSession session, Object request) {
        synchronized (gate) {
            for (Host host : hosts.values()) {
                for (Handoff handoff : host.handoffs.values()) {
                    if (handoff.configs != null && handoff.sessionId.equals(session.getSessionId())) {
                        handoff.configs.put(session, JsonRpcClient.getObjectMapper().valueToTree(request));
                    }
                }
            }
        }
    }

    void disconnect() {
        disconnect(null);
    }

    void disconnect(ServerHostApi rpc) {
        ArrayList<Host> active;
        synchronized (gate) {
            active = new ArrayList<>(hosts.values().stream().filter(host -> rpc == null || host.rpc == rpc).toList());
        }
        for (Host host : active) {
            endHost(host, new AhpHostExit(host.id, HostExitReason.OWNERDISCONNECTED, null,
                    "SDK owner connection closed before listener cleanup could be acknowledged"));
        }
    }

    private CompletableFuture<Map<String, String>> materialize(JsonNode params) throws IOException {
        String hostId = text(params, "hostId");
        String handoffId = text(params, "handoffId");
        JsonNode config = params.required("config");
        if (!(config instanceof ObjectNode expected)) {
            throw new IllegalArgumentException("AHP factory config must be an object");
        }
        String sessionId = text(expected, "sessionId");
        boolean resume = params.path("resume").asBoolean(false);
        Handoff handoff;
        AhpSessionAuthentication authentication;
        synchronized (gate) {
            Host host = hosts.get(hostId);
            if (host == null || host.exited) {
                throw new IllegalStateException("AHP host is not active");
            }
            JsonNode context = params.required("authentication");
            String mode = text(context, "mode");
            var policy = host.options.getAuthentication();
            String identity = "none".equals(mode) ? null : text(context, "identity");
            if (!policy.mode().equals(mode) || ("none".equals(mode) && context.has("identity"))
                    || ("github".equals(mode) && (identity == null || identity.isBlank()))
                    || ("custom".equals(mode) && !policy.requireIdentity().equals(identity))) {
                throw new IllegalArgumentException("AHP trusted authentication does not match host policy");
            }
            if (expected.has("authentication")) {
                throw new IllegalArgumentException("AHP authentication must not be session configuration");
            }
            authentication = new AhpSessionAuthentication(mode, identity);
            if (host.handoffs.containsKey(handoffId)) {
                throw new IllegalStateException("Duplicate AHP session handoff");
            }
            if (resume ? host.options.getResumeSession() == null : host.options.getCreateSession() == null) {
                throw new IllegalStateException("No application AHP session factory configured");
            }
            handoff = new Handoff(host, handoffId, sessionId, expected.deepCopy(),
                    resume ? sessions.get(sessionId) : null);
            host.handoffs.put(handoffId, handoff);
        }
        try {
            ObjectNode converted = expected.deepCopy();
            rename(converted, "configDir", "configDirectory");
            rename(converted, "suppressResumeEvent", "disableResume");
            Supplier<CompletableFuture<CopilotSession>> factory;
            if (resume) {
                converted.remove("sessionId");
                var settings = CONFIG_MAPPER.treeToValue(converted, ResumeSessionConfig.class);
                var request = new AhpSessionResumeRequest(sessionId, settings,
                        handoff.cancellation.minimalCompletionStage(), authentication);
                factory = () -> handoff.host.options.getResumeSession().apply(request);
            } else {
                var settings = CONFIG_MAPPER.treeToValue(converted, SessionConfig.class);
                var request = new AhpSessionCreateRequest(settings, handoff.cancellation.minimalCompletionStage(),
                        authentication);
                factory = () -> handoff.host.options.getCreateSession().apply(request);
            }
            CompletableFuture.supplyAsync(() -> {
                synchronized (gate) {
                    if (handoff.released) {
                        throw new CancellationException("AHP session participation ended");
                    }
                }
                return Objects.requireNonNull(factory.get(), "AHP factory returned a null future");
            }, executor).thenCompose(future -> future)
                    .whenComplete((session, error) -> finish(handoff, session, error));
        } catch (Exception error) {
            finish(handoff, null, error);
        }
        return handoff.response;
    }

    private void finish(Handoff handoff, CopilotSession session, Throwable failure) {
        Throwable error = failure == null ? null : unwrap(failure);
        boolean released;
        synchronized (gate) {
            released = handoff.released;
            if (!released) {
                handoff.session = session;
                if (error == null) {
                    try {
                        validate(handoff, session);
                    } catch (IllegalArgumentException invalid) {
                        error = invalid;
                    }
                }
                handoff.configs = null;
            }
        }
        if (released) {
            if (session != null) {
                notifyReleased(handoff, session);
            }
            if (error != null && !(error instanceof CancellationException)) {
                LOG.log(Level.WARNING, "AHP factory failed after participation ended", error);
            }
        } else if (error != null) {
            handoff.response.completeExceptionally(error);
            release(handoff);
        } else {
            handoff.response.complete(Map.of("sessionId", handoff.sessionId));
        }
    }

    private void validate(Handoff handoff, CopilotSession session) {
        if (session == null || !handoff.sessionId.equals(session.getSessionId())
                || sessions.get(handoff.sessionId) != session) {
            throw new IllegalArgumentException(
                    "AHP factory must return the exact requested session object registered on the owning client");
        }
        JsonNode actual = handoff.configs.get(session);
        if (actual == null && session == handoff.retained) {
            return;
        }
        if (actual == null) {
            throw new IllegalArgumentException("AHP factory must create or resume its session during this handoff");
        }
        ObjectNode expected = handoff.expected.deepCopy();
        rename(expected, "suppressResumeEvent", "disableResume");
        rename(expected, "enableExperimentalMode", "isExperimentalMode");
        rename(expected, "enableMcpApps", "requestMcpApps");
        for (var field : expected.properties()) {
            // These default-false flags are omitted by the session request builder.
            if (!actual.has(field.getKey()) && field.getValue().isBoolean() && !field.getValue().booleanValue()
                    && (field.getKey().equals("streaming") || field.getKey().equals("requestMcpApps")
                            || field.getKey().equals("disableResume"))) {
                continue;
            }
            if (!contains(actual.get(field.getKey()), field.getValue())) {
                throw new IllegalArgumentException("AHP factory changed host-selected config." + field.getKey());
            }
        }
    }

    private static boolean contains(JsonNode actual, JsonNode expected) {
        if (!expected.isObject()) {
            return actual != null && expected.equals((left, right) -> {
                if (left.isNumber() && right.isNumber()) {
                    return left.decimalValue().compareTo(right.decimalValue());
                }
                return left.equals(right) ? 0 : 1;
            }, actual);
        }
        if (actual == null || !actual.isObject()) {
            return false;
        }
        for (var field : expected.properties()) {
            if (!contains(actual.get(field.getKey()), field.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void release(Handoff handoff) {
        CopilotSession session;
        synchronized (gate) {
            if (handoff.released) {
                return;
            }
            handoff.released = true;
            handoff.host.handoffs.remove(handoff.id, handoff);
            handoff.configs = null;
            session = handoff.session;
            handoff.session = null;
        }
        handoff.response.completeExceptionally(new CancellationException("AHP session participation ended"));
        handoff.cancellation.completeAsync(() -> null);
        if (session != null) {
            notifyReleased(handoff, session);
        }
    }

    private void endHost(Host host, AhpHostExit info) {
        ArrayList<Handoff> active;
        ArrayList<Authentication> authentications;
        synchronized (gate) {
            if (host.exited) {
                return;
            }
            host.exited = true;
            hosts.remove(host.id, host);
            active = new ArrayList<>(host.handoffs.values());
            authentications = new ArrayList<>(host.authentications.values());
            host.authentications.clear();
        }
        authentications.forEach(AhpHostManager::cancelAuthentication);
        active.forEach(this::release);
        if (info != null && host.options.getOnExit() != null) {
            notifyCallback(() -> host.options.getOnExit().apply(info));
        }
    }

    private void notifyReleased(Handoff handoff, CopilotSession session) {
        if (handoff.host.options.getOnSessionReleased() != null) {
            handoff.cancellation
                    .thenRun(() -> notifyCallback(() -> handoff.host.options.getOnSessionReleased().apply(session)));
        }
    }

    private static void notifyCallback(Supplier<CompletableFuture<Void>> callback) {
        // Late results can arrive after the owning client's executor has shut down.
        CompletableFuture
                .supplyAsync(() -> Objects.requireNonNull(callback.get(), "AHP callback returned a null future"))
                .thenCompose(future -> future).whenComplete((ignored, error) -> {
                    if (error != null) {
                        LOG.log(Level.WARNING, "AHP lifecycle callback failed", unwrap(error));
                    }
                });
    }

    private static String text(JsonNode object, String field) {
        JsonNode value = object.required(field);
        if (!value.isTextual() || value.textValue().isEmpty()) {
            throw new IllegalArgumentException("AHP " + field + " must be a nonempty string");
        }
        return value.textValue();
    }

    private static void rename(ObjectNode object, String source, String target) {
        JsonNode value = object.remove(source);
        if (value != null) {
            object.set(target, value);
        }
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
}

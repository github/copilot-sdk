package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AutoTierSwitchFailureReason;
import com.github.copilot.generated.SessionAutoTierSwitchFailedEvent;
import com.github.copilot.generated.SessionModelChangeEvent;
import com.github.copilot.generated.rpc.ModelSwitchAutoTierStatus;
import com.github.copilot.generated.rpc.SessionEventLogReadParams;
import com.github.copilot.rpc.AutoTier;
import com.github.copilot.rpc.CapiSessionOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SetModelOptions;

/**
 * End-to-end coverage for Auto tier switching, mirroring
 * {@code nodejs/test/e2e/auto_tier.e2e.test.ts}.
 * <p>
 * The runtime stages an Auto routing preference rather than applying it
 * immediately: a request stays unclaimed until a later turn using the
 * {@code auto} model mints a usable model and token pair. These tests read the
 * staged state back through {@code model.getCurrent()}, so they assert what the
 * runtime actually recorded rather than what the SDK serialized.
 */
class AutoTierIT {

    private static final String MODEL_ID = "auto";

    private static E2ETestContext ctx;

    @BeforeAll
    static void setUp() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    private static com.github.copilot.generated.rpc.AutoTier pendingAutoTier(CopilotSession session) throws Exception {
        return session.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS).pendingAutoTier();
    }

    private static CopilotSession createAutoSession(CopilotClient client) throws Exception {
        return client
                .createSession(
                        new SessionConfig().setModel(MODEL_ID).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                .get(30, TimeUnit.SECONDS);
    }

    @Test
    void shouldStageAndResetAutoTierPreference() throws Exception {
        ctx.configureForTest("auto_tier", "should_stage_and_reset_auto_tier_preference");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = createAutoSession(client);
            try {
                assertNull(pendingAutoTier(session));

                var staged = session.setAutoTier(AutoTier.EFFICIENCY).get(30, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.PENDING, staged.status());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, staged.pendingAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, pendingAutoTier(session));

                // A second request replaces the first and reports the one it displaced.
                var superseded = session.setAutoTier(AutoTier.FAST).get(30, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.PENDING, superseded.status());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, superseded.pendingAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, superseded.supersededAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, pendingAutoTier(session));

                var replacedFast = session.setAutoTier(AutoTier.INTELLIGENCE).get(30, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.PENDING, replacedFast.status());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.INTELLIGENCE, replacedFast.pendingAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, replacedFast.supersededAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.INTELLIGENCE, pendingAutoTier(session));

                // A null tier returns the session to provider-default routing. The status
                // is `unchanged` because provider-default was already the committed
                // preference; the request's effect is cancelling the staged one.
                var reset = session.setAutoTier(null).get(30, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.UNCHANGED, reset.status());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.INTELLIGENCE, reset.supersededAutoTier());
                assertNull(pendingAutoTier(session));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void shouldPreserveAutoTierWhenSetModelOmitsIt() throws Exception {
        ctx.configureForTest("auto_tier", "should_preserve_auto_tier_when_set_model_omits_it");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = createAutoSession(client);
            try {
                session.setAutoTier(AutoTier.BALANCE).get(30, TimeUnit.SECONDS);
                assertEquals(com.github.copilot.generated.rpc.AutoTier.BALANCE, pendingAutoTier(session));

                // Omitting the preference leaves the staged one alone.
                session.setModel(new SetModelOptions().setModel(MODEL_ID)).get(30, TimeUnit.SECONDS);
                assertEquals(com.github.copilot.generated.rpc.AutoTier.BALANCE, pendingAutoTier(session));

                // Supplying a tier replaces it.
                session.setModel(new SetModelOptions().setModel(MODEL_ID).setAutoTier(AutoTier.FAST)).get(30,
                        TimeUnit.SECONDS);
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, pendingAutoTier(session));

                // Requesting a reset clears it. Omission, an explicit tier, and a reset
                // are three distinct outcomes.
                session.setModel(new SetModelOptions().setModel(MODEL_ID).setResetAutoTier(true)).get(30,
                        TimeUnit.SECONDS);
                assertNull(pendingAutoTier(session));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void shouldRestoreAndOverrideFastAutoTierOnColdResume() throws Exception {
        ctx.configureForTest("auto_tier", "should_restore_and_override_fast_auto_tier_on_cold_resume");

        String fastSessionId;
        String tierlessSessionId;
        try (CopilotClient client = ctx.createClient()) {
            try (CopilotSession fastSession = client.createSession(
                    new SessionConfig().setModel(MODEL_ID).setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setCapi(new CapiSessionOptions().setAutoTier(AutoTier.FAST)
                                    .setEnableWebSocketResponses(false)))
                    .get(30, TimeUnit.SECONDS);
                    CopilotSession tierlessSession = client
                            .createSession(new SessionConfig().setModel(MODEL_ID)
                                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setCapi(new CapiSessionOptions().setEnableWebSocketResponses(false)))
                            .get(30, TimeUnit.SECONDS)) {
                fastSessionId = fastSession.getSessionId();
                tierlessSessionId = tierlessSession.getSessionId();

                fastSession
                        .sendAndWait(new MessageOptions().setPrompt("Reply with exactly AUTO_TIER_COLD_RESUME_READY."))
                        .get(30, TimeUnit.SECONDS);
                tierlessSession
                        .sendAndWait(new MessageOptions().setPrompt("Reply with exactly AUTO_TIER_TIERLESS_READY."))
                        .get(30, TimeUnit.SECONDS);

                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST,
                        fastSession.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS).autoTier());
                assertNull(tierlessSession.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS).autoTier());
            }
            client.stop().get(30, TimeUnit.SECONDS);
        }

        try (CopilotClient restoredClient = ctx.createClient()) {
            try (CopilotSession restoredFast = restoredClient
                    .resumeSession(fastSessionId,
                            new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);
                    CopilotSession restoredTierless = restoredClient
                            .resumeSession(tierlessSessionId,
                                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                            .get(30, TimeUnit.SECONDS)) {
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST,
                        restoredFast.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS).autoTier());
                assertNull(restoredTierless.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS).autoTier());
            }
            restoredClient.stop().get(30, TimeUnit.SECONDS);
        }

        try (CopilotClient overrideClient = ctx.createClient();
                CopilotSession overridden = overrideClient
                        .resumeSession(fastSessionId, new ResumeSessionConfig().setModel(MODEL_ID)
                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCapi(new CapiSessionOptions()
                                        .setAutoTier(AutoTier.BALANCE).setEnableWebSocketResponses(false)))
                        .get(30, TimeUnit.SECONDS)) {
            assertEquals(com.github.copilot.generated.rpc.AutoTier.BALANCE,
                    overridden.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS).autoTier());
        }
    }

    @Test
    void shouldCommitFastAutoTierAfterSuccessfulTurn() throws Exception {
        ctx.configureForTest("auto_tier", "should_commit_fast_auto_tier_after_successful_turn");

        try (CopilotClient client = ctx.createClient();
                CopilotSession session = client
                        .createSession(new SessionConfig().setModel(MODEL_ID)
                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCapi(new CapiSessionOptions()
                                        .setAutoTier(AutoTier.EFFICIENCY).setEnableWebSocketResponses(false)))
                        .get(30, TimeUnit.SECONDS)) {
            var modelChangedFuture = new CompletableFuture<SessionModelChangeEvent>();
            try (var subscription = session.on(SessionModelChangeEvent.class, event -> {
                if (event.getData().autoTier() == com.github.copilot.generated.AutoTier.FAST) {
                    modelChangedFuture.complete(event);
                }
            })) {
                var staged = session.setAutoTier(AutoTier.FAST).get(30, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.PENDING, staged.status());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, staged.effectiveAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, staged.pendingAutoTier());

                var beforeTurn = session.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS);
                assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, beforeTurn.autoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, beforeTurn.pendingAutoTier());

                session.sendAndWait(new MessageOptions().setPrompt("Reply with exactly AUTO_TIER_FAST_COMMITTED."))
                        .get(30, TimeUnit.SECONDS);

                var modelChanged = modelChangedFuture.get(30, TimeUnit.SECONDS);
                assertEquals(MODEL_ID, modelChanged.getData().previousModel());
                assertEquals(MODEL_ID, modelChanged.getData().newModel());
                assertEquals(com.github.copilot.generated.AutoTier.EFFICIENCY,
                        modelChanged.getData().previousAutoTier());
                assertEquals(com.github.copilot.generated.AutoTier.FAST, modelChanged.getData().autoTier());

                var committed = session.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS);
                assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, committed.autoTier());
                assertNull(committed.pendingAutoTier());
                assertNull(committed.activatingAutoTier());
            }
        }
    }

    @Test
    void shouldPreserveEffectiveTierWhenFastActivationFails() throws Exception {
        ctx.configureForTest("auto_tier", "should_preserve_effective_tier_when_fast_activation_fails");

        String sessionId;
        try (CopilotClient client = ctx.createClient()) {
            try (CopilotSession session = client
                    .createSession(new SessionConfig().setModel(MODEL_ID)
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCapi(new CapiSessionOptions()
                                    .setAutoTier(AutoTier.EFFICIENCY).setEnableWebSocketResponses(false)))
                    .get(30, TimeUnit.SECONDS)) {
                sessionId = session.getSessionId();
                session.sendAndWait(new MessageOptions().setPrompt("Reply with exactly AUTO_TIER_INITIAL_READY."))
                        .get(30, TimeUnit.SECONDS);

                var failureFuture = new CompletableFuture<SessionAutoTierSwitchFailedEvent>();
                var fastCommitted = new AtomicBoolean();
                try (var failureSubscription = session.on(SessionAutoTierSwitchFailedEvent.class,
                        failureFuture::complete);
                        var modelChangeSubscription = session.on(SessionModelChangeEvent.class, event -> {
                            if (event.getData().autoTier() == com.github.copilot.generated.AutoTier.FAST) {
                                fastCommitted.set(true);
                            }
                        })) {
                    var staged = session.setAutoTier(AutoTier.FAST).get(30, TimeUnit.SECONDS);
                    assertEquals(ModelSwitchAutoTierStatus.PENDING, staged.status());
                    assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, staged.effectiveAutoTier());
                    assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, staged.pendingAutoTier());

                    session.sendAndWait(
                            new MessageOptions().setPrompt("Reply with exactly AUTO_TIER_FAILURE_RECOVERED."))
                            .get(30, TimeUnit.SECONDS);

                    var failure = failureFuture.get(30, TimeUnit.SECONDS);
                    assertTrue(failure.getEphemeral());
                    assertEquals(com.github.copilot.generated.AutoTier.EFFICIENCY,
                            failure.getData().effectiveAutoTier());
                    assertEquals(com.github.copilot.generated.AutoTier.FAST, failure.getData().requestedAutoTier());
                    assertEquals(AutoTierSwitchFailureReason.REQUEST_FAILED, failure.getData().reason());
                    assertFalse(fastCommitted.get());

                    var current = session.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS);
                    assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, current.autoTier());
                    assertNull(current.pendingAutoTier());
                    assertNull(current.activatingAutoTier());
                }
            }
            client.stop().get(30, TimeUnit.SECONDS);
        }

        try (CopilotClient resumedClient = ctx.createClient();
                CopilotSession resumed = resumedClient
                        .resumeSession(sessionId,
                                new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS)) {
            var resumedCurrent = resumed.getRpc().model.getCurrent().get(30, TimeUnit.SECONDS);
            assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, resumedCurrent.autoTier());
            assertNull(resumedCurrent.pendingAutoTier());
            assertNull(resumedCurrent.activatingAutoTier());

            var persisted = resumed.getRpc().eventLog
                    .read(new SessionEventLogReadParams(null, null, 100L, 0L, null, null, null, null, false))
                    .get(30, TimeUnit.SECONDS);
            assertFalse(persisted.events().stream().anyMatch(SessionAutoTierSwitchFailedEvent.class::isInstance));
        }
    }
}

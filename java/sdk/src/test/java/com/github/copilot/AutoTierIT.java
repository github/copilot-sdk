package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.ModelSwitchAutoTierStatus;
import com.github.copilot.rpc.AutoTier;
import com.github.copilot.rpc.PermissionHandler;
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
                var superseded = session.setAutoTier(AutoTier.INTELLIGENCE).get(30, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.PENDING, superseded.status());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.INTELLIGENCE, superseded.pendingAutoTier());
                assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, superseded.supersededAutoTier());
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
                session.setModel(new SetModelOptions().setModel(MODEL_ID).setAutoTier(AutoTier.INTELLIGENCE)).get(30,
                        TimeUnit.SECONDS);
                assertEquals(com.github.copilot.generated.rpc.AutoTier.INTELLIGENCE, pendingAutoTier(session));

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
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.E2ETestContext;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.rpc.HistoryRewindMode;
import com.github.copilot.generated.rpc.HistoryRewindOutcome;
import com.github.copilot.generated.rpc.SessionHistoryListRewindPointsResult;
import com.github.copilot.generated.rpc.SessionHistoryPreviewRewindParams;
import com.github.copilot.generated.rpc.SessionHistoryRewindParams;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

@AllowCopilotExperimental
class RewindIT {

    private static final String FILE_NAME = "rewind-sdk.txt";
    private static final String ORIGINAL_FILE_CONTENT = "Original rewind content";
    private static final String PREPARED_FILE_CONTENT = "Prepared rewind content";
    private static final String FILE_CONTENT = "SDK rewind content";

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
    void shouldRestoreTrackedFileAndConversation() throws Exception {
        ctx.configureForTest("rewind", "should_restore_tracked_file_and_conversation");
        Path filePath = ctx.getWorkDir().resolve(FILE_NAME);
        Files.writeString(filePath, ORIGINAL_FILE_CONTENT);

        try (CopilotClient client = ctx.createClient();
                CopilotSession session = client.createSession(new SessionConfig().setModel("claude-sonnet-5")
                        .setEnableFileChangeTracking(true).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS)) {
            AssistantMessageEvent ready = session
                    .sendAndWait(new MessageOptions().setPrompt("Use the edit tool to replace the exact contents of "
                            + FILE_NAME + " from " + ORIGINAL_FILE_CONTENT + " to " + PREPARED_FILE_CONTENT
                            + ". After the tool succeeds, reply with exactly SDK_REWIND_READY."), 30_000)
                    .get(60, TimeUnit.SECONDS);
            assertNotNull(ready);
            assertEquals("SDK_REWIND_READY", ready.getData().content());
            assertEquals(PREPARED_FILE_CONTENT, Files.readString(filePath));

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Use the edit tool to replace the exact contents of "
                            + FILE_NAME + " from " + PREPARED_FILE_CONTENT + " to " + FILE_CONTENT
                            + ". After the tool succeeds, reply with exactly SDK_REWIND_DONE."), 30_000)
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertEquals("SDK_REWIND_DONE", response.getData().content());
            assertEquals(FILE_CONTENT, Files.readString(filePath));

            SessionHistoryListRewindPointsResult rewindPoints = waitForRewindPoints(session);
            assertTrue(Boolean.TRUE.equals(rewindPoints.fileChangeTrackingEnabled()));
            assertEquals(2, rewindPoints.points().size());
            var rewindPoint = rewindPoints.points().get(1);
            assertTrue(Boolean.TRUE.equals(rewindPoint.turnChangedFiles()));
            assertTrue(Boolean.TRUE.equals(rewindPoint.canRestoreFiles()));
            assertEquals(1L, rewindPoint.fileCount());

            var preview = session.getRpc().history
                    .previewRewind(new SessionHistoryPreviewRewindParams(null, rewindPoint.eventId()))
                    .get(10, TimeUnit.SECONDS);
            assertTrue(Boolean.TRUE.equals(preview.available()));
            assertEquals(1, preview.files().size());
            assertSamePath(filePath, preview.files().get(0).path());

            var rewind = session.getRpc().history.rewind(new SessionHistoryRewindParams(null, rewindPoint.eventId(),
                    HistoryRewindMode.CONVERSATION_AND_FILES)).get(10, TimeUnit.SECONDS);
            assertEquals(HistoryRewindOutcome.SUCCESS, rewind.outcome());
            assertTrue(rewind.eventsRemoved() != null && rewind.eventsRemoved() > 0);
            assertEquals(1, rewind.restoredFiles().size());
            assertSamePath(filePath, rewind.restoredFiles().get(0));
            assertEquals(PREPARED_FILE_CONTENT, Files.readString(filePath));

            var events = session.getMessages().get(10, TimeUnit.SECONDS);
            assertTrue(events.stream().noneMatch(event -> event.getId().toString().equals(rewindPoint.eventId())));
        }
    }

    private static SessionHistoryListRewindPointsResult waitForRewindPoints(CopilotSession session) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        SessionHistoryListRewindPointsResult result;
        do {
            result = session.getRpc().history.listRewindPoints().get(10, TimeUnit.SECONDS);
            if (result.unavailableReason() == null && result.points().size() == 2
                    && Boolean.TRUE.equals(result.points().get(1).turnChangedFiles())
                    && Boolean.TRUE.equals(result.points().get(1).canRestoreFiles())) {
                return result;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        } while (System.nanoTime() < deadline);

        assertNull(result.unavailableReason(), "Timed out waiting for rewind points to become available");
        assertTrue(result.points().size() >= 2, "Timed out waiting for both rewind points");
        assertTrue(Boolean.TRUE.equals(result.points().get(1).turnChangedFiles()),
                "Timed out waiting for the edit turn to capture file changes");
        assertTrue(Boolean.TRUE.equals(result.points().get(1).canRestoreFiles()),
                "Timed out waiting for rewind file restoration to become available");
        return result;
    }

    private static void assertSamePath(Path expected, String actual) {
        String expectedPath = expected.toAbsolutePath().normalize().toString();
        String actualPath = Path.of(actual).toAbsolutePath().normalize().toString();
        if (System.getProperty("os.name").startsWith("Windows")) {
            assertTrue(expectedPath.equalsIgnoreCase(actualPath));
        } else {
            assertEquals(expectedPath, actualPath);
        }
    }
}

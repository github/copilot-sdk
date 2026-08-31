/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const FILE_NAME = "rewind-sdk.txt";
const FILE_CONTENT = "SDK rewind content";

function expectSamePath(actual: string, expected: string): void {
    const actualPath = resolve(actual);
    const expectedPath = resolve(expected);
    if (process.platform === "win32") {
        expect(actualPath.toLowerCase()).toBe(expectedPath.toLowerCase());
    } else {
        expect(actualPath).toBe(expectedPath);
    }
}

describe("Rewind", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    // TODO(cli-1.0.81): Re-enable when Windows file-change tracking records built-in create tool writes.
    it.skipIf(process.platform === "win32")(
        "should restore tracked file and conversation",
        async () => {
            const filePath = join(workDir, FILE_NAME);
            const session = await client.createSession({
                model: "claude-sonnet-4.5",
                enableFileChangeTracking: true,
                onPermissionRequest: approveAll,
            });

            try {
                const response = await session.sendAndWait({
                    prompt: `Use the create tool to create ${FILE_NAME} containing exactly ${FILE_CONTENT}. After the tool succeeds, reply with exactly SDK_REWIND_DONE.`,
                });

                expect(response?.data.content).toBe("SDK_REWIND_DONE");
                expect(existsSync(filePath)).toBe(true);
                expect(readFileSync(filePath, "utf8")).toBe(FILE_CONTENT);

                let rewindPoints = await session.rpc.history.listRewindPoints();
                const deadline = Date.now() + 30_000;
                while (
                    Date.now() < deadline &&
                    (rewindPoints.unavailableReason !== undefined ||
                        !rewindPoints.points[0]?.canRestoreFiles)
                ) {
                    await new Promise((resolveDelay) => setTimeout(resolveDelay, 100));
                    rewindPoints = await session.rpc.history.listRewindPoints();
                }

                expect(rewindPoints.unavailableReason).toBeUndefined();
                expect(rewindPoints.fileChangeTrackingEnabled).toBe(true);
                expect(rewindPoints.points).toHaveLength(1);
                const rewindPoint = rewindPoints.points[0];
                expect(rewindPoint.canRestoreFiles).toBe(true);
                expect(rewindPoint.fileCount).toBe(1);

                const preview = await session.rpc.history.previewRewind({
                    eventId: rewindPoint.eventId,
                });
                expect(preview.available).toBe(true);
                expect(preview.files).toHaveLength(1);
                expectSamePath(preview.files[0].path, filePath);

                const rewind = await session.rpc.history.rewind({
                    eventId: rewindPoint.eventId,
                    mode: "conversation-and-files",
                });
                expect(rewind.outcome).toBe("success");
                expect(rewind.eventsRemoved).toBeGreaterThan(0);
                expect(rewind.restoredFiles).toHaveLength(1);
                expectSamePath(rewind.restoredFiles[0], filePath);
                expect(existsSync(filePath)).toBe(false);

                const events = await session.getEvents();
                expect(events.some((event) => event.id === rewindPoint.eventId)).toBe(false);
            } finally {
                await session.disconnect();
            }
        }
    );
});

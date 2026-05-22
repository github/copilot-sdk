import { afterEach, describe, expect, it, vi } from "vitest";
import { CopilotClient } from "../src/client.js";
import { approveAll } from "../src/index.js";
import { createCanvas, joinSession } from "../src/extension.js";
import { defaultJoinSessionPermissionHandler } from "../src/types.js";

describe("joinSession", () => {
    const originalSessionId = process.env.SESSION_ID;

    afterEach(() => {
        if (originalSessionId === undefined) {
            delete process.env.SESSION_ID;
        } else {
            process.env.SESSION_ID = originalSessionId;
        }
        vi.restoreAllMocks();
    });

    it("defaults onPermissionRequest to no-result", async () => {
        process.env.SESSION_ID = "session-123";
        const resumeSession = vi
            .spyOn(CopilotClient.prototype, "resumeSession")
            .mockResolvedValue({} as any);

        await joinSession({ tools: [] });

        const [, config] = resumeSession.mock.calls[0]!;
        expect(config.onPermissionRequest).toBeDefined();
        expect(config.onPermissionRequest).toBe(defaultJoinSessionPermissionHandler);
        const result = await Promise.resolve(
            config.onPermissionRequest!({ kind: "write" }, { sessionId: "session-123" })
        );
        expect(result).toEqual({ kind: "no-result" });
        expect(config.suppressResumeEvent).toBe(true);
    });

    it("preserves an explicit onPermissionRequest handler", async () => {
        process.env.SESSION_ID = "session-123";
        const resumeSession = vi
            .spyOn(CopilotClient.prototype, "resumeSession")
            .mockResolvedValue({} as any);

        await joinSession({ onPermissionRequest: approveAll, suppressResumeEvent: false });

        const [, config] = resumeSession.mock.calls[0]!;
        expect(config.onPermissionRequest).toBe(approveAll);
        expect(config.suppressResumeEvent).toBe(false);
    });

    it("exports the canvas helper from the extension surface", () => {
        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            onOpen: () => ({ url: "https://example.test/counter" }),
        });

        expect(canvas.declaration.id).toBe("counter");
    });
});

import { afterEach, describe, expect, it, vi } from "vitest";
import { CopilotClient } from "../src/client.js";
import { approveAll } from "../src/index.js";
import { joinSession } from "../src/extension.js";

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
        const result = await Promise.resolve(
            config.onPermissionRequest!({ kind: "write" }, { sessionId: "session-123" })
        );
        expect(result).toEqual({ kind: "no-result" });
        expect(config.disableResume).toBe(true);
    });

    it("preserves an explicit onPermissionRequest handler", async () => {
        process.env.SESSION_ID = "session-123";
        const resumeSession = vi
            .spyOn(CopilotClient.prototype, "resumeSession")
            .mockResolvedValue({} as any);

        await joinSession({ onPermissionRequest: approveAll, disableResume: false });

        const [, config] = resumeSession.mock.calls[0]!;
        expect(config.onPermissionRequest).toBe(approveAll);
        expect(config.disableResume).toBe(false);
    });
});

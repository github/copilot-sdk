import { afterEach, describe, expect, it, vi } from "vitest";
import { CopilotClient } from "../src/client.js";
import { approveAll } from "../src/index.js";
import { createCanvas, defineFactory, defineWorkflow, joinSession } from "../src/extension.js";
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
        const resumeForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as any);

        await joinSession({ tools: [] });

        const [, config] = resumeForExtension.mock.calls[0]!;
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
        const resumeForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as any);

        await joinSession({ onPermissionRequest: approveAll, suppressResumeEvent: false });

        const [, config] = resumeForExtension.mock.calls[0]!;
        expect(config.onPermissionRequest).toBe(approveAll);
        expect(config.suppressResumeEvent).toBe(false);
    });

    it("forwards the requested environment variables and keeps them off the resume config", async () => {
        process.env.SESSION_ID = "session-123";
        const resumeForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as any);

        await joinSession({
            requestedEnvironmentVariables: ["GITHUB_TOKEN", "MY_SECRET"],
            tools: [],
        });

        const [, config, , extensionOptions] = resumeForExtension.mock.calls[0]!;
        expect(extensionOptions).toEqual({
            requestedEnvironmentVariables: ["GITHUB_TOKEN", "MY_SECRET"],
        });
        expect(config).not.toHaveProperty("requestedEnvironmentVariables");
    });

    it("requests no environment variables when the option is omitted or empty", async () => {
        process.env.SESSION_ID = "session-123";
        const resumeForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as any);

        await joinSession({ tools: [] });
        await joinSession({ requestedEnvironmentVariables: [], tools: [] });

        expect(resumeForExtension.mock.calls[0]![3]).toBeUndefined();
        // An empty list means the same as omitting the option, so it must not put
        // an environment request on the wire either.
        expect(resumeForExtension.mock.calls[1]![3]).toBeUndefined();
    });

    it("forwards workflow contributions independently", async () => {
        process.env.SESSION_ID = "session-123";
        const resumeForExtension = vi
            .spyOn(CopilotClient.prototype, "resumeSessionForExtension")
            .mockResolvedValue({} as any);
        const workflow = defineWorkflow({
            meta: { name: "review", description: "Review", phases: [] },
            run: async () => null,
        });

        await joinSession({ workflows: [workflow] });

        expect(resumeForExtension.mock.calls[0]![2]).toEqual({
            factories: undefined,
            workflows: [workflow],
        });
    });

    it("rejects mixed factory and workflow contributions", async () => {
        process.env.SESSION_ID = "session-123";
        const factory = defineFactory({
            meta: { name: "legacy", description: "Legacy", phases: [] },
            run: async () => null,
        });
        const workflow = defineWorkflow({
            meta: { name: "review", description: "Review", phases: [] },
            run: async () => null,
        });

        await expect(
            joinSession({
                // @ts-expect-error Contribution generations are mutually exclusive.
                factories: [factory],
                workflows: [workflow],
            })
        ).rejects.toThrow("cannot register both factories and workflows");
    });

    it("exports the canvas helper from the extension surface", () => {
        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "A counter canvas",
            open: () => ({ url: "https://example.test/counter" }),
        });

        expect(canvas.declaration.id).toBe("counter");
    });
});

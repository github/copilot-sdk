/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "child_process";
import * as fs from "fs";
import * as path from "path";
import { fileURLToPath } from "url";
import { describe, expect, it } from "vitest";
import {
    approveAll,
    RuntimeConnection,
    type SessionEvent,
    type SkillProvider,
    type SkillProviderDescriptor,
} from "../../src/index.js";
import { createSdkTestContext, getLegacyCliPathForTests } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

interface ProvidedSkill {
    descriptor: SkillProviderDescriptor;
    read: () => string | null;
}

/** An in-memory provider that records every callback the runtime makes. */
class TestSkillProvider implements SkillProvider {
    readonly calls: string[] = [];

    constructor(private readonly skills: ProvidedSkill[]) {}

    get reads(): string[] {
        return this.calls
            .filter((call) => call.startsWith("read:"))
            .map((call) => call.slice("read:".length));
    }

    listSkills(): SkillProviderDescriptor[] {
        this.calls.push("list");
        return this.skills.map((skill) => skill.descriptor);
    }

    readSkill(name: string): string | null {
        this.calls.push(`read:${name}`);
        return this.skills.find((skill) => skill.descriptor.name === name)?.read() ?? null;
    }
}

function skill(name: string, description: string, markdown: string): ProvidedSkill {
    return { descriptor: { name, description }, read: () => markdown };
}

describe("Skill providers", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should load provider skill lazily through skill tool", async () => {
        // Body-only content: the catalog descriptor supplies all of the metadata.
        const provider = new TestSkillProvider([
            skill(
                "provider-lookup",
                "Reports the provider lookup verification word.",
                "# Provider lookup\n\nThe verification word is TANGERINE_QUARTZ_19. Reply with it.\n"
            ),
        ]);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillProvider: provider,
        });

        try {
            const { skills } = await session.rpc.skills.list();
            const listed = skills.find((s) => s.name === "provider-lookup");
            expect(listed).toMatchObject({ source: "sdk", enabled: true });
            expect(listed?.path ?? "").toBe("");
            expect(provider.reads).toEqual([]);

            const message = await session.sendAndWait({
                prompt: "Use the skill tool to load the provider-lookup skill, then reply with its verification word.",
            });

            expect(provider.reads).toEqual(["provider-lookup"]);
            // Validate the final assistant response arrived (guards against truncated captures)
            expect(message?.data.content).toContain("TANGERINE_QUARTZ_19");
        } finally {
            await session.disconnect();
        }
    });

    it("should load provider and file based skills together", async () => {
        const skillsDir = path.join(workDir, "file-skills");
        fs.mkdirSync(path.join(skillsDir, "file-notes"), { recursive: true });
        fs.writeFileSync(
            path.join(skillsDir, "file-notes", "SKILL.md"),
            "---\nname: file-notes\ndescription: Reports the file notes verification word.\n---\n\nThe file notes verification word is MAPLE_FALCON_27.\n"
        );
        // Frontmatter may restate catalog metadata and is the only source of allowed-tools.
        const provider = new TestSkillProvider([
            skill(
                "provider-audit",
                "Reports the provider audit verification word.",
                "---\nname: provider-audit\nallowed-tools: view\n---\n\nThe provider audit verification word is COBALT_HERON_58.\n"
            ),
        ]);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillDirectories: [skillsDir],
            skillProvider: provider,
        });

        try {
            const { skills } = await session.rpc.skills.list();
            const fileSkill = skills.find((s) => s.name === "file-notes");
            const providerSkill = skills.find((s) => s.name === "provider-audit");
            expect(fileSkill?.source).not.toBe("sdk");
            expect(fileSkill?.path).toBeTruthy();
            expect(providerSkill?.source).toBe("sdk");

            const message = await session.sendAndWait({
                prompt: "Use the skill tool to load the file-notes skill and the provider-audit skill, then reply with both verification words.",
            });

            expect(provider.reads).toEqual(["provider-audit"]);
            expect(message?.data.content).toContain("MAPLE_FALCON_27");
            // Validate the final assistant response arrived (guards against truncated captures)
            expect(message?.data.content).toContain("COBALT_HERON_58");
        } finally {
            await session.disconnect();
        }
    });

    it("should rebind skill provider on resume", async () => {
        const original = new TestSkillProvider([
            skill(
                "rebind-check",
                "Reports the rebind verification word.",
                "The rebind verification word is AMBER_ALPHA_11.\n"
            ),
        ]);
        const replacement = new TestSkillProvider([
            skill(
                "rebind-check",
                "Reports the rebind verification word.",
                "The rebind verification word is BRONZE_BETA_22.\n"
            ),
        ]);
        const first = await client.createSession({
            onPermissionRequest: approveAll,
            skillProvider: original,
        });
        const sessionId = first.sessionId;
        // A completed turn persists the session so that it can be resumed after disconnecting.
        await first.sendAndWait({
            prompt: "Without using any tools or skills, reply with exactly REBIND_READY.",
        });
        await first.disconnect();
        expect(original.reads).toEqual([]);
        const originalCallsBeforeResume = original.calls.length;

        const session = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            skillProvider: replacement,
        });

        try {
            const message = await session.sendAndWait({
                prompt: "Use the skill tool to load the rebind-check skill, then reply with its verification word.",
            });

            expect(replacement.reads).toEqual(["rebind-check"]);
            expect(original.calls.length).toBe(originalCallsBeforeResume);
            // Validate the final assistant response arrived (guards against truncated captures)
            expect(message?.data.content).toContain("BRONZE_BETA_22");
            expect(message?.data.content).not.toContain("AMBER_ALPHA_11");
        } finally {
            await session.disconnect();
        }
    });

    it("should report provider read failure without leaking details", async () => {
        const secret = "PROVIDER_SECRET_7F3A9C";
        const provider = new TestSkillProvider([
            {
                descriptor: {
                    name: "broken-lookup",
                    description: "Reports the broken lookup verification word.",
                },
                read: () => {
                    throw new Error(`database unavailable: ${secret}`);
                },
            },
        ]);
        const events: SessionEvent[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillProvider: provider,
            onEvent: (event) => events.push(event),
        });

        try {
            const message = await session.sendAndWait({
                prompt: "Use the skill tool to load the broken-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
            });

            expect(provider.reads).toContain("broken-lookup");
            const failures = events.filter(
                (event) => event.type === "tool.execution_complete" && !event.data.success
            );
            expect(failures).toHaveLength(1);
            expect(JSON.stringify(events)).not.toContain(secret);
            // Validate the final assistant response arrived (guards against truncated captures)
            expect(message?.data.content).toContain("LOAD_FAILED");
        } finally {
            await session.disconnect();
        }
    });

    it("should report missing provider skill as not found", async () => {
        const provider = new TestSkillProvider([
            {
                descriptor: {
                    name: "vanished-lookup",
                    description: "Reports the vanished lookup verification word.",
                },
                read: () => null,
            },
        ]);
        const events: SessionEvent[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillProvider: provider,
            onEvent: (event) => events.push(event),
        });

        try {
            const message = await session.sendAndWait({
                prompt: "Use the skill tool to load the vanished-lookup skill. If loading fails, reply with exactly LOAD_FAILED.",
            });

            expect(provider.reads).toContain("vanished-lookup");
            const failures = events.filter(
                (event) => event.type === "tool.execution_complete" && !event.data.success
            );
            expect(failures).toHaveLength(1);
            expect(JSON.stringify(failures[0])).toMatch(/not found/i);
            // Validate the final assistant response arrived (guards against truncated captures)
            expect(message?.data.content).toContain("LOAD_FAILED");
        } finally {
            await session.disconnect();
        }
    });

    it("should keep provider dormant when skills disabled", async () => {
        const provider = new TestSkillProvider([
            skill("dormant-lookup", "Never listed.", "Never read.\n"),
        ]);
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            enableSkills: false,
            skillProvider: provider,
        });

        try {
            await session.rpc.skills.ensureLoaded();
            const { skills } = await session.rpc.skills.list();

            expect(skills.filter((s) => s.source === "sdk")).toEqual([]);
            expect(provider.calls).toEqual([]);
        } finally {
            await session.disconnect();
        }
    });

    it("should unbind provider when resumed without one", async () => {
        const provider = new TestSkillProvider([
            skill("unbound-lookup", "Reports the unbound lookup word.", "Unbound.\n"),
        ]);
        const first = await client.createSession({
            onPermissionRequest: approveAll,
            skillProvider: provider,
        });
        const before = await first.rpc.skills.list();
        expect(before.skills.some((s) => s.name === "unbound-lookup")).toBe(true);
        const callsBeforeResume = provider.calls.length;

        // Resume while the provider is still bound so the unbind is observable.
        const session = await client.resumeSession(first.sessionId, {
            onPermissionRequest: approveAll,
        });

        try {
            await session.rpc.skills.reload();
            const { skills } = await session.rpc.skills.list();

            expect(skills.filter((s) => s.source === "sdk")).toEqual([]);
            expect(provider.calls.length).toBe(callsBeforeResume);
        } finally {
            await session.disconnect();
        }
    });

    it("should cancel a blocked provider call when the session disconnects", async () => {
        let entered!: () => void;
        const started = new Promise<void>((resolve) => (entered = resolve));
        let cancelled!: () => void;
        const aborted = new Promise<void>((resolve) => (cancelled = resolve));
        const provider: SkillProvider = {
            listSkills: ({ signal }) =>
                new Promise<never>((_resolve, reject) => {
                    signal.addEventListener(
                        "abort",
                        () => {
                            cancelled();
                            reject(signal.reason);
                        },
                        { once: true }
                    );
                    entered();
                }),
            readSkill: () => null,
        };
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            skillProvider: provider,
        });

        // The list RPC fails once the binding is removed; only the provider's
        // cancellation matters here.
        void session.rpc.skills.list().catch(() => {});
        await started;
        await session.disconnect();

        let timer: NodeJS.Timeout | undefined;
        await Promise.race([
            aborted,
            new Promise<never>((_resolve, reject) => {
                timer = setTimeout(
                    () => reject(new Error("provider signal was not aborted after disconnect")),
                    10_000
                );
            }),
        ]).finally(() => clearTimeout(timer));
    });

    it("should reject skill provider for cloud sessions", async () => {
        const provider = new TestSkillProvider([
            skill("cloud-lookup", "Never listed.", "Never read.\n"),
        ]);

        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                cloud: {},
                skillProvider: provider,
            })
        ).rejects.toThrow("Skill providers are not supported for cloud sessions.");

        expect(provider.calls).toEqual([]);
    });
});

describe("Skill providers with extensions", async () => {
    const cliDistDirectory = process.env.COPILOT_EXTENSION_SDK_PATH
        ? path.dirname(process.env.COPILOT_EXTENSION_SDK_PATH)
        : path.dirname(await getLegacyCliPathForTests());
    const sdkDistDirectory = path.resolve(
        path.dirname(fileURLToPath(import.meta.url)),
        "..",
        "..",
        "dist"
    );
    // Runs on the default runtime, which is the native runtime in CI.
    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            connection: RuntimeConnection.forStdio(),
            env: { COPILOT_CLI_ENABLED_FEATURE_FLAGS: "EXTENSIONS" },
            extensionLaunchProvider: {
                resolve: async (request) => ({
                    launch: {
                        executable: "node",
                        args: [path.join(cliDistDirectory, "preloads", "extension_bootstrap.mjs")],
                        env: {
                            COPILOT_CLI_DIST_DIR: cliDistDirectory,
                            EXTENSION_PATH: request.modulePath,
                        },
                    },
                }),
            },
        },
    });

    it(
        "should keep provider skills when an extension joins the session",
        { timeout: 120_000 },
        async () => {
            execFileSync("git", ["init", "--quiet"], { cwd: workDir });
            const provider = new TestSkillProvider([
                skill("extension-safe-lookup", "Reports the extension-safe word.", "Safe.\n"),
            ]);
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                requestExtensions: true,
                extensionSdkPath: sdkDistDirectory,
                skillProvider: provider,
            });

            try {
                const before = await session.rpc.skills.list();
                expect(before.skills.filter((s) => s.source === "sdk").map((s) => s.name)).toEqual([
                    "extension-safe-lookup",
                ]);

                const extensionDir = path.join(
                    workDir,
                    ".github",
                    "extensions",
                    "provider-observer"
                );
                const joinedFile = path.join(extensionDir, "joined");
                fs.mkdirSync(extensionDir, { recursive: true });
                fs.writeFileSync(
                    path.join(extensionDir, "extension.mjs"),
                    `import { writeFileSync } from "node:fs";
import { joinSession } from "@github/copilot-sdk/extension";
await joinSession({});
writeFileSync(new URL("./joined", import.meta.url), "joined");
setInterval(() => {}, 60_000);
`
                );
                await session.rpc.extensions.reload();
                // An extension joins by resuming the session, which must not
                // unbind the owner's provider.
                await retry(
                    "wait for the extension to join the session",
                    async () => {
                        expect(fs.existsSync(joinedFile)).toBe(true);
                    },
                    300,
                    100
                );
                await session.rpc.skills.reload();
                const { skills } = await session.rpc.skills.list();

                expect(skills.filter((s) => s.source === "sdk").map((s) => s.name)).toEqual([
                    "extension-safe-lookup",
                ]);
            } finally {
                await session.disconnect();
            }
        }
    );
});

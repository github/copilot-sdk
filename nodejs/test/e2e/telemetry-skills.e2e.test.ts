/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdir, readFile, rename, writeFile } from "fs/promises";
import { join } from "path";
import { fileURLToPath } from "url";
import { describe, expect, it } from "vitest";
import { approveAll, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

interface Span {
    type: string;
    name: string;
    traceId: string;
    spanId: string;
    parentSpanId: string;
    kind: number;
    attributes: Record<string, unknown>;
    status: { code: number };
    events: { name: string; attributes: Record<string, unknown> }[];
}

async function spansFrom(path: string): Promise<Span[]> {
    const lines = (await readFile(path, "utf8")).split("\n").filter((line) => line.trim());
    return lines.map((line): Span => JSON.parse(line)).filter((entry) => entry.type === "span");
}

async function createSkill(
    workDir: string,
    directory = "skills",
    name = "review",
    description = "OTel skill description sentinel"
): Promise<string> {
    const root = join(workDir, directory, name);
    await mkdir(join(root, "references"), { recursive: true });
    await mkdir(join(root, "scripts"), { recursive: true });
    await writeFile(
        join(root, "SKILL.md"),
        `---\nname: ${name}\ndescription: ${description}\n---\nFollow the user's explicit instructions. The verification word is OTEL_SKILL_ORCHID.\n`
    );
    await writeFile(join(root, "references", "policy.txt"), "OTEL_RESOURCE_SAPPHIRE");
    await writeFile(
        join(root, "scripts", "check.cjs"),
        'console.log("OTEL_SCRIPT_AMBER"); process.exit(0);\n'
    );
    return root;
}

function expectNoLegacySkillFields(attributes: Record<string, unknown>): void {
    for (const name of [
        "github.copilot.tool.parameters.skill_name",
        "github.copilot.skill.name",
        "github.copilot.skill.path",
    ]) {
        expect(attributes).not.toHaveProperty(name);
    }
}

const shell = process.platform === "win32" ? "powershell" : "bash";

function expectShellLauncher(span: Span): void {
    expect(span.attributes["process.executable.name"]).toMatch(
        process.platform === "win32" ? /^(pwsh|powershell)(\.exe)?$/i : /^bash$/
    );
}

for (const captureContent of [false, true]) {
    describe(`Skill telemetry capture ${captureContent}`, async () => {
        const { createClient, workDir } = await createSdkTestContext({
            copilotClientOptions: {
                connection: RuntimeConnection.forStdio(),
            },
        });

        it(
            `exports loader resource and script facts capture ${captureContent}`,
            { timeout: 90_000 },
            async () => {
                const telemetryFile = "loader-telemetry.jsonl";
                await using client = createClient({
                    telemetry: { exporterType: "file", filePath: telemetryFile, captureContent },
                });
                const root = await createSkill(workDir);
                await writeFile(join(workDir, "policy.txt"), "OTEL_UNRELATED_POLICY");
                const session = await client.createSession({
                    enableConfigDiscovery: false,
                    skillDirectories: [join(workDir, "skills")],
                    onPermissionRequest: approveAll,
                });
                const calls: { id: string; tool: string }[] = [];
                const exits = new Map<string, number>();
                session.on("tool.execution_start", (event) => {
                    calls.push({ id: event.data.toolCallId, tool: event.data.toolName });
                });
                session.on("tool.execution_complete", (event) => {
                    const exit = event.data.shellExecution?.exitCode;
                    if (exit !== undefined) {
                        exits.set(event.data.toolCallId, exit);
                    }
                });
                const response = await session.sendAndWait(
                    {
                        prompt:
                            "Use the skill tool to load review. Then use view to read skills/review/references/policy.txt. " +
                            "Then use the shell tool in sync mode to run exactly: node './skills/review/scripts/check.cjs'. " +
                            "Then use view to read policy.txt in the working directory, outside the skill. " +
                            "Then use view to read skills/review/SKILL.md, and use view to list the skills/review directory. " +
                            "After all six calls finish, reply with exactly OTEL_SKILL_FLOW_DONE.",
                    },
                    90_000
                );
                expect(response?.data.content).toContain("OTEL_SKILL_FLOW_DONE");
                await session.disconnect();
                expect(await client.stop()).toEqual([]);

                const spans = await spansFrom(join(workDir, telemetryFile));
                const tools = spans.filter(
                    (span) => span.attributes["gen_ai.operation.name"] === "execute_tool"
                );
                expect(tools).toHaveLength(6);
                expect(tools.every((span) => span.kind === 0)).toBe(true);
                expect(calls.filter((call) => call.tool === "skill")).toHaveLength(1);
                for (const call of calls) {
                    expect(
                        tools.filter((span) => span.attributes["gen_ai.tool.call.id"] === call.id)
                    ).toHaveLength(1);
                }
                const loader = tools.find(
                    (span) => span.attributes["gen_ai.tool.name"] === "skill"
                )!;
                expect(loader.name).toBe("execute_tool skill review");
                expect(loader.attributes["gen_ai.skill.name"]).toBe("review");
                const reader = tools.find(
                    (span) =>
                        span.attributes["gen_ai.skill.resource.name"] === "references/policy.txt"
                )!;
                expect(reader.name).toBe("execute_tool view review references/policy.txt");
                const command = tools.find(
                    (span) => span.attributes["gen_ai.skill.resource.name"] === "scripts/check.cjs"
                )!;
                expect(command.name).toBe(`execute_tool ${shell} review scripts/check.cjs`);
                expect(command.attributes["process.exit.code"]).toBe(0);
                expect(command.attributes["process.exit.code"]).toBe(
                    exits.get(String(command.attributes["gen_ai.tool.call.id"]))
                );
                expectShellLauncher(command);
                expect(reader.parentSpanId).toBe(loader.parentSpanId);
                expect(command.parentSpanId).toBe(loader.parentSpanId);
                expect(reader.traceId).toBe(loader.traceId);
                expect(command.traceId).toBe(loader.traceId);
                const unrelated = tools.filter(
                    (span) => span.attributes["gen_ai.tool.name"] === "view" && span !== reader
                );
                expect(unrelated).toHaveLength(3);
                for (const generic of unrelated) {
                    expect(generic.name).toBe("execute_tool view");
                    expect(generic.attributes).not.toHaveProperty("gen_ai.skill.name");
                    expect(generic.attributes).not.toHaveProperty("gen_ai.skill.resource.name");
                }
                for (const span of [loader, reader, command]) {
                    expectNoLegacySkillFields(span.attributes);
                    expect(span.attributes["gen_ai.operation.name"]).toBe("execute_tool");
                    if (captureContent) {
                        expect(span.attributes["gen_ai.skill.description"]).toBe(
                            "OTel skill description sentinel"
                        );
                        expect(
                            fileURLToPath(String(span.attributes["gen_ai.skill.source.uri"]))
                        ).toBe(join(root, "SKILL.md"));
                    } else {
                        expect(span.attributes).not.toHaveProperty("gen_ai.skill.description");
                        expect(span.attributes).not.toHaveProperty("gen_ai.skill.source.uri");
                        expect(span.attributes).not.toHaveProperty("process.executable.path");
                        expect(span.attributes).not.toHaveProperty("gen_ai.tool.call.arguments");
                        expect(span.attributes).not.toHaveProperty("gen_ai.tool.call.result");
                    }
                }
                for (const span of spans) {
                    expectNoLegacySkillFields(span.attributes);
                    for (const event of span.events) {
                        expectNoLegacySkillFields(event.attributes);
                    }
                }
                const invocation = spans
                    .flatMap((span) => span.events)
                    .find((event) => event.name === "github.copilot.skill.invoked")!;
                expect(invocation.attributes["gen_ai.skill.name"]).toBe("review");
                expectNoLegacySkillFields(invocation.attributes);
                expect(Boolean(invocation.attributes["github.copilot.skill.content"])).toBe(
                    captureContent
                );
                if (captureContent) {
                    expect(invocation.attributes["gen_ai.skill.description"]).toBe(
                        "OTel skill description sentinel"
                    );
                    expect(
                        fileURLToPath(String(invocation.attributes["gen_ai.skill.source.uri"]))
                    ).toBe(join(root, "SKILL.md"));
                } else {
                    expect(invocation.attributes).not.toHaveProperty("gen_ai.skill.description");
                    expect(invocation.attributes).not.toHaveProperty("gen_ai.skill.source.uri");
                }
            }
        );

        it(
            `exports unknown skill and ordinary zero and nonzero exits capture ${captureContent}`,
            { timeout: 90_000 },
            async () => {
                const telemetryFile = "exit-telemetry.jsonl";
                await using client = createClient({
                    telemetry: { exporterType: "file", filePath: telemetryFile, captureContent },
                });
                await createSkill(workDir);
                await writeFile(
                    join(workDir, "exit.cjs"),
                    "process.exit(Number(process.argv[2]));\n"
                );
                const session = await client.createSession({
                    enableConfigDiscovery: false,
                    skillDirectories: [join(workDir, "skills")],
                    onPermissionRequest: approveAll,
                });
                const exits = new Map<string, number>();
                session.on("tool.execution_complete", (event) => {
                    const exit = event.data.shellExecution?.exitCode;
                    if (exit !== undefined) {
                        exits.set(event.data.toolCallId, exit);
                    }
                });
                const response = await session.sendAndWait(
                    {
                        prompt:
                            "Use the skill tool to attempt to load otel-nonexistent. Do not search for or install it. " +
                            "Then use the shell tool in sync mode to run exactly: node './exit.cjs' 0; exit 0. " +
                            "Then use the shell tool in sync mode to run exactly: node './exit.cjs' 7; exit 7. " +
                            "A nonzero exit is intentional. Do not retry it. Reply with exactly OTEL_EXIT_FLOW_DONE.",
                    },
                    90_000
                );
                expect(response?.data.content).toContain("OTEL_EXIT_FLOW_DONE");
                await session.disconnect();
                expect(await client.stop()).toEqual([]);
                const spans = await spansFrom(join(workDir, telemetryFile));
                const loader = spans.find(
                    (span) => span.attributes["gen_ai.tool.name"] === "skill"
                )!;
                expect(loader.name).toBe("execute_tool skill otel-nonexistent");
                expect(loader.attributes["gen_ai.skill.name"]).toBe("otel-nonexistent");
                expect(loader.attributes).not.toHaveProperty("gen_ai.skill.description");
                expect(loader.attributes).not.toHaveProperty("gen_ai.skill.source.uri");
                expect(loader.status.code).toBe(2);
                const commands = spans.filter(
                    (span) => span.attributes["gen_ai.tool.name"] === shell
                );
                expect(commands).toHaveLength(2);
                expect(commands.map((span) => span.attributes["process.exit.code"]).sort()).toEqual(
                    [0, 7]
                );
                for (const command of commands) {
                    expectShellLauncher(command);
                    expect(command.name).toBe(
                        `execute_tool ${shell} ${command.attributes["process.executable.name"]}`
                    );
                    expect(command.attributes).not.toHaveProperty("gen_ai.skill.name");
                    expect(command.attributes).not.toHaveProperty("gen_ai.skill.resource.name");
                    expect(command.attributes["process.exit.code"]).toBe(
                        exits.get(String(command.attributes["gen_ai.tool.call.id"]))
                    );
                    expect(command.status.code).not.toBe(2);
                }
            }
        );

        it(
            `keeps reloaded skill provenance and child attribution separate capture ${captureContent}`,
            { timeout: 90_000 },
            async () => {
                const telemetryFile = "reload-telemetry.jsonl";
                await using client = createClient({
                    telemetry: { exporterType: "file", filePath: telemetryFile, captureContent },
                });
                const oldRoot = await createSkill(workDir);
                const childRoot = await createSkill(workDir, "skills", "child-review");
                const session = await client.createSession({
                    enableConfigDiscovery: false,
                    skillDirectories: [join(workDir, "skills")],
                    onPermissionRequest: approveAll,
                });
                const childExits = new Map<string, number>();
                const shellStarts: unknown[] = [];
                session.on("tool.execution_start", (event) => {
                    if (event.data.toolName === shell) {
                        shellStarts.push(event.data);
                    }
                });
                session.on("tool.execution_complete", (event) => {
                    const exit = event.data.shellExecution?.exitCode;
                    if (exit !== undefined) {
                        childExits.set(event.data.toolCallId, exit);
                    }
                });
                const first = await session.sendAndWait({
                    prompt: "Use the skill tool to load review, then reply with exactly OTEL_BEFORE_RELOAD_DONE.",
                });
                expect(first?.data.content).toContain("OTEL_BEFORE_RELOAD_DONE");
                const newRoot = join(workDir, "skills", "moved-review");
                await rename(oldRoot, newRoot);
                await writeFile(
                    join(newRoot, "SKILL.md"),
                    "---\nname: review\ndescription: Reloaded OTel description\n---\nFollow the user's explicit instructions.\n"
                );
                const reload = await session.rpc.skills.reload();
                expect(reload.errors).toEqual([]);
                const second = await session.sendAndWait(
                    {
                        prompt:
                            "Use the skill tool to load review again. Then use the task tool with agent_type exactly 'task' and mode 'sync' " +
                            "to load child-review with the skill tool, use view to read skills/child-review/references/policy.txt, " +
                            "then use the shell tool in sync mode to run exactly: node './skills/child-review/scripts/check.cjs'. " +
                            "The task agent should report the resource and script words. After the agent finishes, " +
                            "reply with exactly OTEL_AFTER_RELOAD_DONE.",
                    },
                    90_000
                );
                expect(second?.data.content).toContain("OTEL_AFTER_RELOAD_DONE");
                await session.disconnect();
                expect(await client.stop()).toEqual([]);
                const spans = await spansFrom(join(workDir, telemetryFile));
                const loaders = spans.filter(
                    (span) => span.attributes["gen_ai.tool.name"] === "skill"
                );
                expect(loaders).toHaveLength(3);
                expect(
                    loaders.filter((span) => span.name === "execute_tool skill review")
                ).toHaveLength(2);
                expect(
                    loaders.map((span) => ({
                        name: span.name,
                        agent: span.attributes["gen_ai.agent.name"],
                    }))
                ).toContainEqual({ name: "execute_tool skill child-review", agent: "task" });
                const child = loaders.find(
                    (span) => span.attributes["gen_ai.agent.name"] === "task"
                )!;
                expect(child).toBeDefined();
                expect(child.name).toBe("execute_tool skill child-review");
                const parent = spans.find((span) => span.spanId === child.parentSpanId)!;
                expect(parent.attributes["gen_ai.operation.name"]).toBe("invoke_agent");
                expect(parent.attributes["gen_ai.agent.name"]).toBe("task");
                expect(child.traceId).toBe(parent.traceId);
                const childReader = spans.find(
                    (span) => span.name === "execute_tool view child-review references/policy.txt"
                )!;
                expect(childReader).toBeDefined();
                expect(childReader.parentSpanId).toBe(parent.spanId);
                expect(childReader.traceId).toBe(parent.traceId);
                expect(childReader.attributes["gen_ai.agent.name"]).toBe("task");
                const childCommands = spans.filter(
                    (span) => span.attributes["gen_ai.skill.resource.name"] === "scripts/check.cjs"
                );
                expect(childCommands, JSON.stringify(shellStarts)).toHaveLength(1);
                const childCommand = childCommands[0];
                expect(childCommand.name).toBe(
                    `execute_tool ${shell} child-review scripts/check.cjs`
                );
                expect(childCommand.parentSpanId).toBe(parent.spanId);
                expect(childCommand.traceId).toBe(parent.traceId);
                expect(childCommand.attributes["gen_ai.agent.name"]).toBe("task");
                expectShellLauncher(childCommand);
                expect(childCommand.attributes["process.exit.code"]).toBe(0);
                expect(childCommand.attributes["process.exit.code"]).toBe(
                    childExits.get(String(childCommand.attributes["gen_ai.tool.call.id"]))
                );
                if (captureContent) {
                    expect(
                        loaders
                            .map((span) =>
                                fileURLToPath(String(span.attributes["gen_ai.skill.source.uri"]))
                            )
                            .sort()
                    ).toEqual(
                        [
                            join(oldRoot, "SKILL.md"),
                            join(newRoot, "SKILL.md"),
                            join(childRoot, "SKILL.md"),
                        ].sort()
                    );
                    expect(
                        loaders.find(
                            (span) =>
                                fileURLToPath(
                                    String(span.attributes["gen_ai.skill.source.uri"])
                                ) === join(oldRoot, "SKILL.md")
                        )!.attributes["gen_ai.skill.description"]
                    ).toBe("OTel skill description sentinel");
                    expect(
                        loaders.find(
                            (span) =>
                                fileURLToPath(
                                    String(span.attributes["gen_ai.skill.source.uri"])
                                ) === join(newRoot, "SKILL.md")
                        )!.attributes["gen_ai.skill.description"]
                    ).toBe("Reloaded OTel description");
                }
            }
        );
    });
}

describe("Preloaded skill telemetry", async () => {
    const telemetryFile = "preload-telemetry.jsonl";
    const { createClient, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            connection: RuntimeConnection.forStdio(),
        },
    });
    it("emits an invocation receipt without fabricating an execute tool span", async () => {
        await using client = createClient({
            telemetry: { exporterType: "file", filePath: telemetryFile, captureContent: true },
        });
        const root = await createSkill(workDir);
        const session = await client.createSession({
            enableConfigDiscovery: false,
            skillDirectories: [join(workDir, "skills")],
            customAgents: [
                {
                    name: "preloaded-review",
                    description: "Preload review",
                    prompt: "Follow the user.",
                    skills: ["review"],
                },
            ],
            agent: "preloaded-review",
            onPermissionRequest: approveAll,
        });
        const response = await session.sendAndWait({
            prompt: "Do not use any tools. Report the verification word from your preloaded review skill and then OTEL_PRELOAD_DONE.",
        });
        expect(response?.data.content).toContain("OTEL_PRELOAD_DONE");
        expect(response?.data.content).toContain("OTEL_SKILL_ORCHID");
        await session.disconnect();
        expect(await client.stop()).toEqual([]);
        const spans = await spansFrom(join(workDir, telemetryFile));
        expect(
            spans.filter((span) => span.attributes["gen_ai.operation.name"] === "execute_tool")
        ).toHaveLength(0);
        const agent = spans.find(
            (span) => span.attributes["gen_ai.operation.name"] === "invoke_agent"
        )!;
        expect(agent.attributes["gen_ai.agent.name"]).toBe("preloaded-review");
        const invocations = agent.events.filter(
            (event) => event.name === "github.copilot.skill.invoked"
        );
        expect(invocations).toHaveLength(1);
        const invocation = invocations[0];
        expect(invocation.attributes["gen_ai.skill.name"]).toBe("review");
        expect(invocation.attributes["gen_ai.skill.description"]).toBe(
            "OTel skill description sentinel"
        );
        expect(fileURLToPath(String(invocation.attributes["gen_ai.skill.source.uri"]))).toBe(
            join(root, "SKILL.md")
        );
        expect(invocation.attributes["github.copilot.skill.content"]).toContain(
            "OTEL_SKILL_ORCHID"
        );
        expectNoLegacySkillFields(invocation.attributes);
    });
});

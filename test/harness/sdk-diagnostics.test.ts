/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtemp, mkdir, writeFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it, onTestFinished } from "vitest";
import { readDiagnosticTail, sanitizeDiagnosticLine } from "./sdk-diagnostics.mjs";

const prefix = "2026-10-05T12:00:00Z [DEBUG] [rust:sdk_diagnostics] ";

describe("SDK failure diagnostics", () => {
    it("projects only audited fields and never falls back to raw data", () => {
        expect(
            sanitizeDiagnosticLine(
                `${prefix}sdk pending tool response ${JSON.stringify({
                    generation: 9,
                    request_event: 3,
                    reason: "completed_request",
                    requestId: "PRIVATE_REQUEST",
                    sessionId: "PRIVATE_SESSION",
                    tool: "PRIVATE_TOOL",
                    path: "/PRIVATE_PATH",
                    error: "PRIVATE_ERROR",
                    payload: "PRIVATE_PROMPT",
                    credential: "PRIVATE_CREDENTIAL",
                    endpoint: "PRIVATE_ENDPOINT",
                })}`,
            ),
        ).toBe('sdk pending tool response {"generation":9,"reason":"completed_request"}');
        expect(
            sanitizeDiagnosticLine(
                `${prefix}sdk pending transition ${JSON.stringify({
                    generation: 9,
                    request_event: 3,
                    event_count: 4,
                    completion_present: true,
                    kind: "permission",
                    origin: "client_response",
                    terminal_cancelled: false,
                    requestId: "PRIVATE_REQUEST",
                })}`,
            ),
        ).toBe(
            'sdk pending transition {"generation":9,"event_count":4,"kind":"permission","origin":"client_response","terminal_cancelled":false}',
        );
        expect(
            sanitizeDiagnosticLine(
                `${prefix}sdk pending tool delivery ${JSON.stringify({
                    generation: 9,
                    origin: "client_response",
                    accepted: true,
                    success: true,
                    failed: false,
                    requestId: "PRIVATE_REQUEST",
                    result: "PRIVATE_RESULT",
                    error: "PRIVATE_ERROR",
                })}`,
            ),
        ).toBe('sdk pending tool delivery {"generation":9,"origin":"client_response","accepted":true,"success":true,"failed":false}');
        for (const line of [
            `${prefix}PRIVATE_MESSAGE {"generation":9}`,
            `${prefix}sdk pending tool response {"reason":"PRIVATE_REASON"}`,
            `${prefix}sdk pending tool response {"generation":"PRIVATE_ID"}`,
            `${prefix}sdk pending tool response {"reason":"accepted"} PRIVATE_TRAILER`,
            `${prefix}sdk pending tool response {PRIVATE_MALFORMED`,
            `${prefix}sdk pending tool response {"PRIVATE_FIELD":"PRIVATE_VALUE"}`,
            `${prefix}sdk pending tool response {"generation":null}`,
            `${prefix}sdk pending tool response {"generation":{}}`,
            `${prefix}sdk pending tool response {"generation":1e100}`,
            `${prefix}sdk pending tool delivery {"origin":"PRIVATE_ORIGIN"}`,
            `PRIVATE_PREFIX ${prefix}sdk pending tool response {"generation":9}`,
        ]) {
            expect(sanitizeDiagnosticLine(line)).toBeUndefined();
        }
    });

    it("bounds the combined tail across files and preserves only audited native records", async () => {
        const home = await mkdtemp(join(tmpdir(), "sdk-diagnostics-"));
        onTestFinished(() => rm(home, { recursive: true, force: true }));
        await mkdir(join(home, "logs"));
        for (const file of ["first.log", "second.log"]) {
            await writeFile(
                join(home, "logs", file),
                Array.from(
                    { length: 150 },
                    (_, generation) =>
                        `${prefix}sdk pending stale drain ${JSON.stringify({ generation, expected_generation: 1, secret: "PRIVATE" })}`,
                ).join("\n"),
            );
        }
        await writeFile(
            join(home, "logs", "third.log"),
            [
                `${prefix}sdk detach {"phase":"received","connection":2,"dispatch":1,"content":"PRIVATE"}`,
                `${prefix}sdk detach {"phase":"dispatch_complete","connection":2,"dispatch":1,"failed":false,"elapsed_ms":4}`,
                '[sdk_diagnostics] sdk detach {"phase":"received","content":"PRIVATE"}',
            ].join("\n"),
        );
        const tail = await readDiagnosticTail(home);
        expect(tail).toHaveLength(200);
        expect(tail[0]).toContain('"generation":102');
        expect(tail.at(-2)).toBe('sdk detach {"connection":2,"dispatch":1,"phase":"received"}');
        expect(tail.at(-1)).toBe('sdk detach {"connection":2,"dispatch":1,"elapsed_ms":4,"failed":false,"phase":"dispatch_complete"}');
        expect(tail.join("\n")).not.toContain("PRIVATE");
        expect(tail.join("\n")).not.toContain(home);
    });

    it("preserves telemetry correlation and fractional shell timings without content", () => {
        const cases = [
            [
                "sdk otel dispatch",
                {
                    event_kind: "assistant_message",
                    event_sequence: 8,
                    scope: "child",
                    child_present: true,
                    chat_present: true,
                    child_span_handle: 2,
                    chat_span_handle: 3,
                },
            ],
            [
                "sdk assistant message",
                {
                    scope: "child",
                    child_present: true,
                    chat_present: true,
                    parent_chat_present: true,
                    child_span_handle: 2,
                    chat_span_handle: 3,
                    part_count: 2,
                    text_count: 1,
                    reasoning_count: 1,
                    capture_responses: true,
                },
            ],
            [
                "sdk chat close",
                {
                    scope: "child",
                    operation: "chat",
                    reason: "subagent_completed",
                    status: "unset",
                    child_span_handle: 2,
                    chat_span_handle: 3,
                    part_count: 2,
                    text_count: 1,
                    reasoning_count: 1,
                    capture_responses: true,
                },
            ],
            [
                "sdk child completion",
                {
                    phase: "closed",
                    operation: "invoke_agent",
                    reason: "subagent_completed",
                    status: "unset",
                    child_span_handle: 2,
                },
            ],
            [
                "sdk shell wait",
                {
                    generation: 2,
                    current_generation: 2,
                    wait_generation: 1,
                    current_wait_generation: 1,
                    sandbox: true,
                    pid: 123,
                    status: "running",
                    exit_code_present: false,
                    output_bytes: 7,
                    reader_backlog_count: 2,
                    pending_stdout_bytes: 4,
                    pending_stderr_bytes: 3,
                    publications_closed: false,
                    phase: "deadline",
                    elapsed_ms: 30000.125,
                    timeout_ms: 30000,
                },
            ],
            [
                "sdk shell exit",
                {
                    generation: 2,
                    sandbox: false,
                    phase: "observed",
                    exit_code_present: true,
                    exit_code: -1,
                    elapsed_ms: 0.125,
                },
            ],
            [
                "sdk shell reader",
                {
                    generation: 2,
                    sandbox: false,
                    phase: "finished",
                    stream: "stderr",
                    elapsed_ms: 0.25,
                },
            ],
            [
                "sdk shell drain",
                {
                    generation: 2,
                    sandbox: false,
                    phase: "aborted",
                    reader_count: 2,
                    reader_unjoined: 1,
                    backpressured: true,
                    elapsed_ms: 1500.125,
                },
            ],
            [
                "sdk shell completion",
                {
                    generation: 2,
                    current_generation: 3,
                    sandbox: false,
                    status: "running",
                    phase: "finish_stale_generation",
                    lifecycle_error: false,
                },
            ],
        ] as const;
        for (const [message, fields] of cases) {
            const safe = sanitizeDiagnosticLine(
                `${prefix}${message} ${JSON.stringify({
                    ...fields,
                    command: "PRIVATE_COMMAND",
                    output: "PRIVATE_OUTPUT",
                    message: "PRIVATE_MESSAGE",
                    span: { content: "PRIVATE_SPAN" },
                    sessionId: "PRIVATE_SESSION",
                    childId: "PRIVATE_CHILD",
                })}`,
            );
            expect(safe).toBeDefined();
            expect(JSON.parse(safe!.slice(message.length + 1))).toEqual(fields);
            expect(safe).not.toContain("PRIVATE");
        }
        for (const fields of [
            { elapsed_ms: -1 },
            { elapsed_ms: "PRIVATE_TIME" },
            { elapsed_ms: null },
            { phase: "PRIVATE_PHASE" },
            { stream: "PRIVATE_STREAM" },
        ]) {
            expect(sanitizeDiagnosticLine(`${prefix}sdk shell reader ${JSON.stringify(fields)}`)).toBeUndefined();
        }
    });
});

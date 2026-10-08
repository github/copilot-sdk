/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createReadStream } from "node:fs";
import { readdir } from "node:fs/promises";
import { join, resolve } from "node:path";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";

// This is a finite projection, not a redactor: target names alone are not a privacy boundary.
const presence = {
    generation: "number",
    event_count: "number",
    resolved_live: "boolean",
    request_present: "boolean",
    completion_present: "boolean",
    tool_present: "boolean",
    orphaned: "boolean",
};
const childParts = {
    child_span_handle: "number",
    chat_span_handle: "number",
    part_count: "number",
    text_count: "number",
    reasoning_count: "number",
    capture_responses: "boolean",
};
const childCloseReasons = ["subagent_completed", "subagent_failed", "agent_turn_close", "dispose"];
const shell = {
    generation: "number",
    sandbox: "boolean",
    pid: "number",
};
const shellState = {
    ...shell,
    current_generation: "number",
    status: ["idle", "running", "completed", "failed"],
    exit_code_present: "boolean",
    exit_code: "number",
    output_bytes: "number",
    reader_backlog_count: "number",
    pending_stdout_bytes: "number",
    pending_stderr_bytes: "number",
    publications_closed: "boolean",
};
const records = {
    "workflow availability decision": { available: "boolean" },
    "sdk pending permission response": {
        ...presence,
        prepared_replay: "boolean",
        aborting: "boolean",
        reason: ["prepared_replay", "completed_request", "unknown_request", "resolved_tool", "accepted"],
    },
    "sdk pending tool response": {
        ...presence,
        reason: ["completed_request", "unknown_request", "non_pending_request", "completed_tool", "accepted"],
    },
    "sdk pending permission delivery": {
        generation: "number",
        settled: "boolean",
        invariant_failure: "boolean",
    },
    "sdk pending tool delivery": {
        generation: "number",
        origin: ["client_response"],
        accepted: "boolean",
        success: "boolean",
        failed: "boolean",
    },
    "sdk pending permission drain planned": {
        generation: "number",
        requests: "number",
        origin: ["suspend", "abort"],
    },
    "sdk pending permission drain completed": {
        generation: "number",
        generation_known: "boolean",
        origin: ["client_response", "cancellation"],
        settled: "boolean",
        invariant_failure: "boolean",
    },
    "sdk pending stale drain": { generation: "number", expected_generation: "number" },
    "sdk pending transition": {
        generation: "number",
        event_count: "number",
        kind: ["permission", "external_tool"],
        origin: [
            "registered",
            "client_response",
            "delivery_failure",
            "suspend",
            "abort",
            "timeout",
            "terminal_response_rejected",
            "prepared_conflict",
            "invalid_response",
            "already_claimed",
        ],
        terminal_cancelled: "boolean",
    },
    "sdk pending resume": {
        connection: "number",
        dispatch: "number",
        warm: "boolean",
        phase: ["begin", "accepted"],
    },
    "sdk pending reap": {
        registration: "number",
        phase: [
            "begin",
            "callbacks_drained",
            "lifecycle_acquired",
            "owner_present",
            "generation_removed",
            "complete",
            "generation_changed",
        ],
    },
    "sdk session shutdown": {
        generation: "number",
        phase: ["begin", "tasks_drained", "terminal_drain_begin", "terminal_drain_complete"],
        failed: "boolean",
        elapsed_ms: "number",
    },
    "sdk detach": {
        connection: "number",
        dispatch: "number",
        elapsed_ms: "number",
        failed: "boolean",
        phase: [
            "received",
            "lifecycle_acquired",
            "already_detached",
            "close_already_started",
            "shared_owner_begin",
            "shared_owner_complete",
            "host_destroy_begin",
            "host_destroy_complete",
            "finalized",
            "dispatch_complete",
        ],
    },
    "sdk detach state": {
        connection: "number",
        dispatch: "number",
        generation: "number",
        active: "boolean",
        attached: "boolean",
    },
    "sdk otel dispatch": {
        event_kind: [
            "assistant_message",
            "model_call_start",
            "assistant_turn_end",
            "subagent_completed",
            "subagent_failed",
        ],
        event_sequence: "number",
        scope: ["parent", "child"],
        child_present: "boolean",
        chat_present: "boolean",
        child_span_handle: "number",
        chat_span_handle: "number",
    },
    "sdk assistant message": {
        ...childParts,
        scope: ["parent", "child"],
        child_present: "boolean",
        chat_present: "boolean",
        parent_chat_present: "boolean",
    },
    "sdk chat close": {
        ...childParts,
        scope: ["parent", "child"],
        operation: ["chat"],
        reason: [...childCloseReasons, "model_call_start", "assistant_turn_end", "parent_turn_close"],
        status: ["unset", "error"],
    },
    "sdk child completion": {
        ...childParts,
        phase: ["begin", "closed"],
        operation: ["invoke_agent"],
        reason: childCloseReasons,
        status: ["unset", "error"],
        child_present: "boolean",
        chat_present: "boolean",
    },
    "sdk shell exit": {
        ...shell,
        phase: [
            "scheduled",
            "started",
            "observed",
            "observe_failed",
            "finalize_started",
            "finalized",
            "finalize_failed",
        ],
        exit_code_present: "boolean",
        exit_code: "number",
        elapsed_ms: "elapsed",
    },
    "sdk shell reader": {
        ...shell,
        phase: ["eof", "error", "finished"],
        stream: ["stdout", "stderr"],
        elapsed_ms: "elapsed",
    },
    "sdk shell drain": {
        ...shell,
        phase: ["drained", "aborted"],
        reader_count: "number",
        reader_unjoined: "number",
        elapsed_ms: "elapsed",
        backpressured: "boolean",
    },
    "sdk shell completion": {
        ...shellState,
        phase: [
            "finish_stale_generation",
            "finish_not_running",
            "finish_publications_closed",
            "publishing",
            "complete_stale_generation",
            "complete_not_running",
            "completed",
        ],
        lifecycle_error: "boolean",
    },
    "sdk shell wait": {
        ...shellState,
        phase: ["interrupted", "stale_generation", "completion", "cancelled", "deadline", "completion_after_deadline"],
        wait_generation: "number",
        current_wait_generation: "number",
        elapsed_ms: "elapsed",
        timeout_ms: "number",
    },
};

export function sanitizeDiagnosticLine(line) {
    if (line.length > 16384) return undefined;
    const match = /^\S+ \[DEBUG\] \[rust:sdk_diagnostics\] ([a-z ]+) (\{.*\})$/.exec(line);
    if (!match || !Object.hasOwn(records, match[1])) return undefined;
    let input;
    try {
        input = JSON.parse(match[2]);
    } catch {
        return undefined;
    }
    if (!input || Array.isArray(input) || typeof input !== "object") return undefined;
    const output = {};
    for (const [key, rule] of Object.entries(records[match[1]])) {
        if (!Object.hasOwn(input, key)) continue;
        const value = input[key];
        const valid = Array.isArray(rule)
            ? rule.includes(value)
            : rule === "number"
              ? Number.isSafeInteger(value)
              : rule === "elapsed"
                ? Number.isFinite(value) && value >= 0
                : typeof value === rule;
        if (!valid) return undefined;
        output[key] = value;
    }
    if (!Object.keys(output).length) return undefined;
    return `${match[1]} ${JSON.stringify(output)}`;
}

export async function readDiagnosticTail(home) {
    const tail = [];
    const append = (line) => {
        const safe = sanitizeDiagnosticLine(line);
        if (safe !== undefined) {
            tail.push(safe);
            if (tail.length > 200) tail.shift();
        }
    };
    let files;
    try {
        files = await readdir(join(home, "logs"));
    } catch (error) {
        if (error.code !== "ENOENT") throw error;
        files = [];
    }
    for (const file of files.sort()) {
        if (!file.endsWith(".log")) continue;
        const lines = createInterface({ input: createReadStream(join(home, "logs", file)), crlfDelay: Infinity });
        for await (const line of lines) append(line);
    }
    return tail;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const tail = await readDiagnosticTail(process.argv[2]);
        console.log(`[SDK diagnostics: last ${tail.length} audited records]\n${tail.join("\n")}`);
    } catch {
        console.error("SDK diagnostics unavailable");
        process.exitCode = 1;
    }
}

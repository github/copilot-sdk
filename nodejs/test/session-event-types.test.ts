/**
 * Regression test for #1156: dedicated session event data/payload types are
 * importable from the package entry point (`@github/copilot-sdk` /
 * `src/index.js`).
 *
 * Before this fix, only the aggregate `SessionEvent` discriminated union was
 * re-exported. The constituent `*Event` wrapper interfaces and their `*Data`
 * payload types lived in `generated/session-events.ts` and could only be
 * reached via a deep import (`@github/copilot-sdk/dist/generated/...`).
 *
 * Most of this file exercises the *type* surface — if these type-only imports
 * compile, the public API exposes the types. The runtime assertions below only
 * validate representative object shapes for those annotations; they do not
 * prove that type-only exports exist at runtime.
 */

import { describe, expect, it } from "vitest";
import type {
    // The aggregate union; must still resolve via the package root.
    SessionEvent,

    // *Data payload types from the v0.3.0 generated session-event schema.
    AssistantMessageData,
    AssistantMessageDeltaData,
    AssistantReasoningData,
    AssistantTurnStartData,
    ErrorData,
    IdleData,
    ResumeData,
    StartData,
    ToolExecutionCompleteData,
    ToolExecutionPartialData,
    ToolExecutionProgressData,
    ToolExecutionStartData,
    UserMessageData,

    // *Event wrapper interfaces.
    AssistantMessageEvent,
    ErrorEvent,
    IdleEvent,
    ResumeEvent,
    StartEvent,
    ToolExecutionCompleteEvent,
    ToolExecutionStartEvent,
    UserMessageEvent,

    // A sample of supporting auxiliary aliases/unions referenced by the
    // *Data shapes — these must also be reachable so that consumers can
    // narrow or annotate intermediate values.
    UserMessageAgentMode,
    UserMessageAttachment,
    WorkingDirectoryContextHostType,
} from "../src/index.js";

/**
 * Type-only helper: forces the compiler to resolve the supplied type
 * parameter. If the type is not exported from `../src/index.js`, the file
 * fails to type-check and the test never runs. There is no runtime body —
 * the helper exists purely to make "is this type importable?" assertions
 * compile-time checked.
 */
function assertImportable<_T>(): void {
    /* no-op; compile-time check only */
}

/**
 * Compile-time mutual-assignability check: passes only when `A` and `B`
 * are structurally equivalent. Used below to pin the package-root
 * `AssistantMessageEvent` (which is explicitly re-exported from
 * `./session.js` and therefore shadows the generated `AssistantMessageEvent`
 * arriving via `export type *`) to the corresponding arm of the generated
 * `SessionEvent` union. If a future schema regen ever caused these two
 * shapes to drift, this assertion would fail to type-check and `npm run
 * typecheck` would surface it before the public API silently changed.
 */
type _AssertEqual<A, B> =
    (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
type _AssistantMessageEventStaysAlignedWithSessionEventUnion = _AssertEqual<
    AssistantMessageEvent,
    Extract<SessionEvent, { type: "assistant.message" }>
>;
const _assistantMessageEventAlignmentCheck: _AssistantMessageEventStaysAlignedWithSessionEventUnion = true;

describe("Session event type exports (#1156)", () => {
    it("exposes the headline ToolExecutionStartData type with a usable shape", () => {
        // This is the specific type called out in issue #1156. The annotation
        // is the compile-time API-surface check; these assertions only validate
        // the representative runtime object shape a consumer would use.
        const data: ToolExecutionStartData = {
            toolCallId: "call-1",
            toolName: "shell",
            arguments: { command: "ls" },
            mcpServerName: "filesystem",
            mcpToolName: "list_dir",
            turnId: "turn-1",
        };

        expect(data.toolName).toBe("shell");
        expect(data.toolCallId).toBe("call-1");
        expect(data.arguments?.command).toBe("ls");
        expect(data.mcpServerName).toBe("filesystem");
        expect(data.mcpToolName).toBe("list_dir");
        expect(data.turnId).toBe("turn-1");
    });

    it("wraps ToolExecutionStartData inside the exported ToolExecutionStartEvent", () => {
        const event: ToolExecutionStartEvent = {
            id: "evt-1",
            parentId: null,
            timestamp: "2026-01-01T00:00:00.000Z",
            type: "tool.execution_start",
            data: {
                toolCallId: "call-1",
                toolName: "shell",
            },
        };

        expect(event.type).toBe("tool.execution_start");
        expect(event.data.toolName).toBe("shell");
        expect(event.parentId).toBeNull();
    });

    it("narrows the aggregate SessionEvent union to a dedicated *Data type", () => {
        const evt: SessionEvent = {
            id: "evt-2",
            parentId: null,
            timestamp: "2026-01-01T00:00:01.000Z",
            type: "tool.execution_start",
            data: {
                toolCallId: "call-2",
                toolName: "shell",
            },
        };

        if (evt.type !== "tool.execution_start") {
            throw new Error("expected tool.execution_start narrowing");
        }

        // After narrowing, `evt.data` must satisfy `ToolExecutionStartData`.
        // Annotating the local with the dedicated *Data type proves the
        // re-export is wired up correctly.
        const data: ToolExecutionStartData = evt.data;
        expect(data.toolCallId).toBe("call-2");
        expect(data.toolName).toBe("shell");
    });

    it("re-exports the full set of *Data and *Event types named in v0.3.0", () => {
        // Compile-time checks: if any of these fail to resolve, the file
        // will not type-check and the test will not be executed.
        assertImportable<AssistantMessageData>();
        assertImportable<AssistantMessageDeltaData>();
        assertImportable<AssistantReasoningData>();
        assertImportable<AssistantTurnStartData>();
        assertImportable<ErrorData>();
        assertImportable<IdleData>();
        assertImportable<ResumeData>();
        assertImportable<StartData>();
        assertImportable<ToolExecutionCompleteData>();
        assertImportable<ToolExecutionPartialData>();
        assertImportable<ToolExecutionProgressData>();
        assertImportable<ToolExecutionStartData>();
        assertImportable<UserMessageData>();

        assertImportable<AssistantMessageEvent>();
        assertImportable<ErrorEvent>();
        assertImportable<IdleEvent>();
        assertImportable<ResumeEvent>();
        assertImportable<StartEvent>();
        assertImportable<ToolExecutionCompleteEvent>();
        assertImportable<ToolExecutionStartEvent>();
        assertImportable<UserMessageEvent>();

        // Supporting auxiliary types referenced by the *Data shapes — these
        // must round-trip through the package root too, otherwise consumers
        // annotating intermediate values would still need a deep import.
        assertImportable<UserMessageAgentMode>();
        assertImportable<UserMessageAttachment>();
        assertImportable<WorkingDirectoryContextHostType>();

        expect(true).toBe(true);
    });
});

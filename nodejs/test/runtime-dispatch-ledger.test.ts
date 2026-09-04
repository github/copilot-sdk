import { describe, expect, it } from "vitest";
import {
    createRuntimeDispatchMarker,
    type ExpectedDispatch,
    validateRuntimeDispatchMarker,
} from "../scripts/runtime-dispatch-ledger.js";

const expected: ExpectedDispatch = {
    channel: "unstable",
    currentRunId: "200",
    mode: "internal",
    runtimeRunId: "100",
    runtimeSha: "a".repeat(40),
    runtimeSource: "github-packages",
    runtimeVersion: "1.2.3-unstable.4",
    sdkRef: "refs/heads/main",
    sdkSha: "b".repeat(40),
    versionOverride: "",
};

function provenance(canonicalRunId: string) {
    return {
        artifact: { expired: false, workflow_run: { id: Number(canonicalRunId) } },
        run: {
            event: "workflow_dispatch",
            head_branch: "main",
            head_sha: expected.sdkSha,
            id: Number(canonicalRunId),
            name: "Runtime-driven Node SDK",
            path: ".github/workflows/runtime-sdk.yml",
            repository: { full_name: "github/copilot-sdk" },
        },
    };
}

describe("runtime dispatch ledger", () => {
    it("creates a canonical marker without adding the runtime run to release identity", () => {
        const marker = createRuntimeDispatchMarker(expected);
        expect(marker.canonicalRunId).toBe("200");
        expect(marker.runtime.runId).toBe("100");
        expect(marker).not.toHaveProperty("sdk.version");
    });

    it("retains ownership for a rerun of the canonical workflow run", () => {
        const marker = createRuntimeDispatchMarker(expected);
        const api = provenance("200");
        expect(validateRuntimeDispatchMarker(marker, api.artifact, api.run, expected)).toBe(
            "owner"
        );
    });

    it("recognizes an exact duplicate", () => {
        const marker = createRuntimeDispatchMarker({ ...expected, currentRunId: "199" });
        const api = provenance("199");
        expect(validateRuntimeDispatchMarker(marker, api.artifact, api.run, expected)).toBe(
            "duplicate"
        );
    });

    it("rejects marker tuple collisions and forged API provenance", () => {
        const marker = createRuntimeDispatchMarker({ ...expected, currentRunId: "199" });
        const api = provenance("199");
        expect(() =>
            validateRuntimeDispatchMarker(marker, api.artifact, api.run, {
                ...expected,
                runtimeSha: "c".repeat(40),
            })
        ).toThrow(/already claimed/);
        expect(() =>
            validateRuntimeDispatchMarker(
                marker,
                { ...api.artifact, workflow_run: { id: 198 } },
                api.run,
                expected
            )
        ).toThrow(/Artifact workflow run ID/);
        expect(() =>
            validateRuntimeDispatchMarker(
                marker,
                api.artifact,
                { ...api.run, path: ".github/workflows/publish.yml" },
                expected
            )
        ).toThrow();
    });
});

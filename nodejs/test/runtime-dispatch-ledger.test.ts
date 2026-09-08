import { describe, expect, it, vi } from "vitest";
import {
    claimRuntimeDispatch,
    createRuntimeDispatchMarker,
    type DispatchLedgerClient,
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
        artifact: {
            expired: false,
            id: 10,
            name: "sdk-runtime-dispatch-100",
            workflow_run: { id: Number(canonicalRunId) },
        },
        run: {
            display_title: "Runtime-driven SDK from runtime run 100",
            event: "workflow_dispatch",
            head_branch: "main",
            head_sha: expected.sdkSha,
            id: Number(canonicalRunId),
            name: "Runtime-driven Node SDK",
            path: ".github/workflows/runtime-sdk.yml",
            repository: { full_name: "github/copilot-sdk" },
            status: "completed",
        },
    };
}

function client(overrides: Partial<DispatchLedgerClient> = {}): DispatchLedgerClient {
    const marker = createRuntimeDispatchMarker({ ...expected, currentRunId: "199" });
    const api = provenance("199");
    return {
        downloadMarker: async () => marker,
        getWorkflowRun: async () => api.run,
        listArtifacts: async () => [api.artifact],
        listWorkflowRuns: async () => [],
        ...overrides,
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

    it("orchestrates exact duplicates and canonical reruns without creating another marker", async () => {
        await expect(claimRuntimeDispatch(expected, client())).resolves.toMatchObject({
            canonicalRunId: "199",
            created: false,
            role: "duplicate",
        });

        const marker = createRuntimeDispatchMarker(expected);
        const api = provenance("200");
        await expect(
            claimRuntimeDispatch(
                expected,
                client({
                    downloadMarker: async () => marker,
                    getWorkflowRun: async () => api.run,
                    listArtifacts: async () => [api.artifact],
                })
            )
        ).resolves.toMatchObject({
            canonicalRunId: "200",
            created: false,
            role: "owner",
        });
    });

    it("rejects multiple exact markers", async () => {
        const api = provenance("199");
        await expect(
            claimRuntimeDispatch(
                expected,
                client({ listArtifacts: async () => [api.artifact, { ...api.artifact, id: 11 }] })
            )
        ).rejects.toThrow("More than one unexpired");
    });

    it("allows a markerless rerun of the same workflow run to claim", async () => {
        const api = provenance("200");
        await expect(
            claimRuntimeDispatch(
                expected,
                client({
                    listArtifacts: async () => [],
                    listWorkflowRuns: async () => [api.run],
                })
            )
        ).resolves.toMatchObject({
            canonicalRunId: "200",
            created: true,
            role: "owner",
        });
    });

    it("retries while an earlier matching run is still initializing", async () => {
        const api = provenance("199");
        const delay = vi.fn(async () => undefined);
        await expect(
            claimRuntimeDispatch(
                expected,
                client({
                    listArtifacts: async () => [],
                    listWorkflowRuns: async () => [{ ...api.run, status: "in_progress" }],
                }),
                { attempts: 2, delay }
            )
        ).rejects.toThrow("still initializing");
        expect(delay).toHaveBeenCalledTimes(1);
    });

    it("resolves a marker that becomes visible during the bounded retry", async () => {
        const api = provenance("199");
        const listArtifacts = vi
            .fn()
            .mockResolvedValueOnce([])
            .mockResolvedValueOnce([api.artifact]);
        await expect(
            claimRuntimeDispatch(
                expected,
                client({
                    listArtifacts,
                    listWorkflowRuns: async () => [api.run],
                }),
                { attempts: 2, delay: async () => undefined }
            )
        ).resolves.toMatchObject({
            canonicalRunId: "199",
            created: false,
            role: "duplicate",
        });
        expect(listArtifacts).toHaveBeenCalledTimes(2);
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

    it("rejects unknown channels at the extracted entry boundary", () => {
        expect(() =>
            createRuntimeDispatchMarker({
                ...expected,
                channel: "invalid" as ExpectedDispatch["channel"],
            })
        ).toThrow("Invalid channel");
    });
});

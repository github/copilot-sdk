import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflow = (name: string) =>
    readFileSync(join(repositoryRoot, ".github", "workflows", name), "utf8");
const publish = workflow("publish.yml");
const runtimeSdk = workflow("runtime-sdk.yml");
const shared = workflow("runtime-backed-node-release.yml");
const ledger = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "runtime-dispatch-ledger.ts"),
    "utf8"
);

describe("normal publishing workflow contract", () => {
    it("remains the stable and prerelease entry without runtime handoff inputs", () => {
        expect(publish).toContain("- latest");
        expect(publish).toContain("- prerelease");
        expect(publish).not.toContain("- unstable");
        expect(publish).not.toContain("runtime_version:");
        expect(publish).not.toContain("runtime_run_id:");
        expect(publish).not.toContain("resume_run_id:");
        expect(publish).not.toContain("runtime-backed-node-release.yml");
        expect(publish).toContain("publish.yml only accepts latest or prerelease");
    });

    it("retains all normal SDK publication paths", () => {
        for (const job of [
            "publish-nodejs:",
            "publish-dotnet:",
            "publish-rust:",
            "publish-python:",
            "publish-java:",
            "github-release:",
        ]) {
            expect(publish).toContain(job);
        }
    });
});

describe("runtime-driven Node SDK entry contract", () => {
    it("owns both strict runtime handoff matrices", () => {
        expect(runtimeSdk).toContain("name: Runtime-driven Node SDK");
        expect(runtimeSdk).toContain("canary:azure:tests-only");
        expect(runtimeSdk).toContain("canary:azure:internal");
        expect(runtimeSdk).toContain("unstable:github-packages:internal");
        expect(runtimeSdk).toContain("runtime_run_id:");
        expect(runtimeSdk).toContain("runtime_source:");
    });

    it("serializes and durably claims each runtime run", () => {
        expect(runtimeSdk).toContain("group: sdk-runtime-dispatch-${{ inputs.runtime_run_id }}");
        expect(runtimeSdk).toContain("cancel-in-progress: false");
        expect(runtimeSdk).toContain("sdk-runtime-dispatch-${{ inputs.runtime_run_id }}");
        expect(runtimeSdk).toContain("More than one unexpired");
        expect(runtimeSdk).toContain("for ATTEMPT in 1 2 3 4 5 6");
        expect(runtimeSdk).toContain("actions/workflows/runtime-sdk.yml/runs");
        expect(runtimeSdk).toContain("runtime-dispatch-ledger.ts validate");
        expect(runtimeSdk).toContain('gh run watch "$CANONICAL_RUN_ID" --exit-status');
        expect(runtimeSdk).toContain("retention-days: 90");
    });

    it("delegates preparation before its separately serialized public publication", () => {
        expect(runtimeSdk).toContain("uses: ./.github/workflows/runtime-backed-node-release.yml");
        expect(runtimeSdk).toContain("scripts/unstable-version.ts");
        expect(runtimeSdk).toContain("group: sdk-runtime-public-unstable");
        expect(runtimeSdk.indexOf("runtime-backed-release:")).toBeLessThan(
            runtimeSdk.indexOf("publish-public:")
        );
        expect(runtimeSdk).toContain("dist/release-manifest.json dist unstable");
    });

    it("only recovers the canonical retained release", () => {
        expect(ledger).toContain("resume_run_id must identify the canonical workflow run");
        expect(runtimeSdk).toContain(
            "run-id: ${{ needs.claim-runtime-dispatch.outputs.canonical_run_id }}"
        );
        expect(runtimeSdk).toContain(
            "Canonical release manifest does not match the claimed runtime dispatch"
        );
    });
});

describe("shared runtime-backed Node pipeline", () => {
    it("enforces the channel, source, and mode matrix again", () => {
        expect(shared).toContain("canary:azure:tests-only");
        expect(shared).toContain("canary:azure:internal");
        expect(shared).toContain("unstable:github-packages:internal");
        expect(shared).not.toContain("registry.npmjs.org");
    });

    it("owns acquisition, cross-platform tests, packaging, and internal verification", () => {
        expect(shared).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(shared).toContain("npm run acquire:runtime-packages");
        expect(shared).toContain("npm run verify:release-packages");
        expect(shared).toContain("publish-manifest");
        expect(shared).toContain("group: sdk-runtime-internal-");
        expect(shared.indexOf("npm run verify:release-packages")).toBeLessThan(
            shared.indexOf("publish-manifest")
        );
    });
});

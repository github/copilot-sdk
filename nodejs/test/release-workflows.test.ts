import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflow = (name: string) =>
    readFileSync(join(repositoryRoot, ".github", "workflows", name), "utf8");
const publish = workflow("publish.yml");
const runtimeSdk = workflow("runtime-sdk.yml");
const runtimeDispatchLedger = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "runtime-dispatch-ledger.ts"),
    "utf8"
);
const acquisitionJob = runtimeSdk.slice(
    runtimeSdk.indexOf("  acquire-runtime:"),
    runtimeSdk.indexOf("  test:")
);
const internalPublicationJob = runtimeSdk.slice(
    runtimeSdk.indexOf("  publish-internal:"),
    runtimeSdk.indexOf("  publish-public:")
);
const publicPublicationJob = runtimeSdk.slice(runtimeSdk.indexOf("  publish-public:"));

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
        expect(publish).toMatch(/- name: Validate release channel\s+working-directory: \.\s+env:/);
        expect(publish).toContain(
            "prerelease namespace is reserved for runtime-driven SDK releases"
        );
        expect(publish).toContain("canary|unstable");
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
    it("contains the runtime-backed implementation without a single-caller reusable workflow", () => {
        expect(
            existsSync(
                join(repositoryRoot, ".github", "workflows", "runtime-backed-node-release.yml")
            )
        ).toBe(false);
    });

    it("owns both strict runtime handoff matrices", () => {
        expect(runtimeSdk).toContain("name: Runtime-driven Node SDK");
        expect(runtimeSdk).toContain("runtime_run_id:");
        expect(runtimeSdk).not.toContain("runtime_source:");
        expect(runtimeDispatchLedger).toContain('expected.channel === "canary"');
        expect(runtimeDispatchLedger).toContain('source: "github-packages"');
        expect(runtimeDispatchLedger).not.toContain("runtimeSource");
    });

    it("serializes and durably claims each runtime run", () => {
        expect(runtimeSdk).toContain("group: sdk-runtime-dispatch-${{ inputs.runtime_run_id }}");
        expect(runtimeSdk).toContain("cancel-in-progress: false");
        expect(runtimeSdk.match(/queue: max/g)).toHaveLength(3);
        expect(runtimeSdk).toContain("sdk-runtime-dispatch-${{ inputs.runtime_run_id }}");
        expect(runtimeSdk).toContain("runtime-dispatch-ledger.ts claim");
        expect(runtimeSdk).toContain("steps.claim.outputs.created == 'true'");
        expect(runtimeSdk).not.toContain("actions/artifacts");
        expect(runtimeSdk).not.toContain("actions/workflows/runtime-sdk.yml/runs");
        expect(runtimeDispatchLedger).toContain("More than one unexpired");
        expect(runtimeDispatchLedger).toContain("attempts ?? 6");
        expect(runtimeDispatchLedger).toContain("actions/workflows/runtime-sdk.yml/runs");
        expect(runtimeDispatchLedger).toContain("canonicalNumericIdPattern");
        expect(runtimeDispatchLedger).not.toContain("process.env[name]?.trim()");
        expect(runtimeSdk).toContain('gh run watch "$CANONICAL_RUN_ID" --exit-status');
        expect(runtimeSdk).toContain("retention-days: 90");
        expect(runtimeSdk).not.toContain("resume_run_id");
    });

    it("delegates preparation before its separately serialized public publication", () => {
        expect(runtimeSdk).toContain("scripts/unstable-version.ts");
        expect(runtimeSdk).toContain("group: sdk-runtime-public-unstable");
        expect(runtimeSdk.indexOf("publish-internal:")).toBeLessThan(
            runtimeSdk.indexOf("publish-public:")
        );
        expect(runtimeSdk).toContain("needs: [claim-runtime-dispatch, plan, publish-internal]");
        expect(runtimeSdk).toContain("dist/release-manifest.json dist unstable");
    });

    it("requires duplicates and failures to use the canonical workflow run", () => {
        expect(runtimeSdk).toContain('gh run watch "$CANONICAL_RUN_ID" --exit-status');
        expect(runtimeSdk).toContain("Re-run that original run");
        expect(runtimeSdk).not.toContain("run-id:");
    });
});

describe("runtime-backed Node release implementation", () => {
    it("enforces the channel, source, and mode matrix", () => {
        expect(runtimeDispatchLedger).toContain('expected.mode === "tests-only"');
        expect(runtimeDispatchLedger).toContain('expected.mode === "internal"');
        expect(runtimeDispatchLedger).toContain("Invalid channel or mode combination");
    });

    it("owns acquisition, cross-platform tests, packaging, and internal verification", () => {
        expect(runtimeSdk).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(runtimeSdk).toContain("npm run acquire:runtime-packages");
        expect(acquisitionJob).toContain("packages: read");
        expect(acquisitionJob).toContain("NODE_AUTH_TOKEN: ${{ github.token }}");
        expect(acquisitionJob).toContain("--registry https://npm.pkg.github.com");
        expect(acquisitionJob).not.toContain("azure/login");
        expect(acquisitionJob).not.toContain("FEED_URL");
        expect(internalPublicationJob).toContain("azure/login");
        expect(internalPublicationJob).toContain('"$FEED_URL" azure');
        expect(internalPublicationJob).not.toContain("registry.npmjs.org");
        expect(publicPublicationJob).toContain("https://registry.npmjs.org public");
        expect(publicPublicationJob).not.toContain("azure/login");
        expect(publicPublicationJob).not.toContain("FEED_URL");
        expect(runtimeSdk).toContain("npm run verify:release-packages");
        expect(runtimeSdk).toContain("publish-manifest");
        expect(runtimeSdk.match(/preflight-package-set/g)).toHaveLength(2);
        expect(runtimeSdk).not.toContain("for PACKAGE in");
        expect(runtimeSdk).toContain("group: sdk-runtime-internal-${{ inputs.channel }}");
        expect(runtimeSdk).not.toContain('"$runtime_path" --version');
        expect(runtimeSdk).not.toContain('"$RUNTIME" --version');
        expect(runtimeSdk).not.toContain("resume_run_id");
        expect(runtimeSdk).toContain("SDK_CHANNEL: ${{ inputs.channel }}");
        expect(runtimeSdk).not.toContain("scripts/get-version.js current");
        expect(runtimeSdk.indexOf("WORKFLOW_CREATED_AT=")).toBeLessThan(
            runtimeSdk.indexOf("scripts/unstable-version.ts")
        );
        expect(runtimeSdk).not.toContain('BASE="${PUBLIC_LATEST%%-*}"');
        expect(runtimeSdk.indexOf("npm run verify:release-packages")).toBeLessThan(
            runtimeSdk.indexOf("publish-manifest")
        );
    });
});

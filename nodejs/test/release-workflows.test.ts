import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflow = (name: string) =>
    readFileSync(join(repositoryRoot, ".github", "workflows", name), "utf8");
const publish = workflow("publish.yml");
const runtimeSdk = workflow("runtime-sdk.yml");
const runtimeReleaseIdentity = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "runtime-release-identity.ts"),
    "utf8"
);
const planJob = runtimeSdk.slice(
    runtimeSdk.indexOf("  plan:"),
    runtimeSdk.indexOf("  acquire-runtime:")
);
const acquisitionJob = runtimeSdk.slice(
    runtimeSdk.indexOf("  acquire-runtime:"),
    runtimeSdk.indexOf("  test:")
);
const testJob = runtimeSdk.slice(runtimeSdk.indexOf("  test:"), runtimeSdk.indexOf("  package:"));
const packageJob = runtimeSdk.slice(
    runtimeSdk.indexOf("  package:"),
    runtimeSdk.indexOf("  publish-internal:")
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
        expect(runtimeReleaseIdentity).toContain('inputs.channel === "canary"');
        expect(runtimeReleaseIdentity).toContain('inputs.mode === "tests-only"');
        expect(runtimeReleaseIdentity).toContain("Invalid channel or mode combination");
        expect(runtimeSdk).toContain("npx tsx scripts/runtime-release-identity.ts");
    });

    it("uses the runtime run ID only as provenance", () => {
        expect(runtimeSdk).toContain(
            'description: "Source runtime workflow run ID for provenance"'
        );
        expect(runtimeSdk).toContain(
            'run-name: "Runtime-driven SDK #${{ github.run_number }} from runtime run ${{ inputs.runtime_run_id }}"'
        );
        expect(runtimeSdk).toContain(
            'description: "Unstable SemVer base for a direct manual run; workflow identity is appended"'
        );
        expect(runtimeSdk).not.toContain("claim-runtime-dispatch");
        expect(runtimeSdk).not.toContain("sdk-runtime-dispatch-");
        expect(runtimeSdk).not.toContain("runtime-dispatch-ledger");
        expect(runtimeSdk).not.toContain("canonical_run");
        expect(runtimeSdk).not.toContain("CANONICAL_RUN_ID");
        expect(runtimeSdk).not.toContain("gh run watch");
        expect(
            existsSync(join(repositoryRoot, "nodejs", "scripts", "runtime-dispatch-ledger.ts"))
        ).toBe(false);
        expect(
            existsSync(join(repositoryRoot, "nodejs", "test", "runtime-dispatch-ledger.test.ts"))
        ).toBe(false);
        expect(runtimeSdk).toContain("cancel-in-progress: false");
        expect(runtimeSdk.match(/queue: max/g)).toHaveLength(2);
        expect(runtimeSdk).not.toContain("resume_run_id");
    });

    it("plans every invocation before separately serialized publication", () => {
        expect(runtimeSdk).toContain("scripts/unstable-version.ts");
        expect(planJob).not.toContain("needs:");
        expect(planJob).not.toContain("if: needs.");
        expect(runtimeSdk).toContain("group: sdk-runtime-public-unstable");
        expect(runtimeSdk.indexOf("publish-internal:")).toBeLessThan(
            runtimeSdk.indexOf("publish-public:")
        );
        expect(runtimeSdk).toContain("needs: [plan, publish-internal]");
        expect(runtimeSdk).toContain("dist/release-manifest.json dist unstable");
        expect(runtimeSdk).not.toContain("needs.claim-runtime-dispatch");
    });
});

describe("runtime-backed Node release implementation", () => {
    it("enforces the channel, source, and mode matrix", () => {
        expect(runtimeReleaseIdentity).toContain('inputs.mode === "tests-only"');
        expect(runtimeReleaseIdentity).toContain('inputs.mode === "internal"');
        expect(runtimeReleaseIdentity).toContain("Invalid channel or mode combination");
        expect(runtimeReleaseIdentity).toContain(
            "Runtime workflow run ID must be a positive canonical integer"
        );
        expect(runtimeReleaseIdentity).toContain("validateRuntimeVersionChannel");
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
        expect(runtimeSdk).not.toContain("preflight-package-set");
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

    it("preserves runtime package modes across every artifact boundary", () => {
        expect(acquisitionJob).toContain(
            'tar -czf "$RUNNER_TEMP/runtime-packages.tar.gz" -C "$RUNNER_TEMP" runtime-packages'
        );
        expect(acquisitionJob).toContain("path: ${{ runner.temp }}/runtime-packages.tar.gz");
        expect(acquisitionJob).not.toContain("path: ${{ runner.temp }}/runtime-packages\n");

        for (const consumer of [testJob, packageJob]) {
            expect(consumer).toContain("path: ${{ runner.temp }}/runtime-package-artifact");
            expect(consumer).toContain(
                'tar -xzf "$RUNNER_TEMP/runtime-package-artifact/runtime-packages.tar.gz" -C "$RUNNER_TEMP"'
            );
            expect(consumer).not.toContain("path: ${{ runner.temp }}/runtime-packages\n");
        }
        expect(testJob.indexOf("Extract validated runtime packages")).toBeLessThan(
            testJob.indexOf("Select the acquired runtime")
        );
        expect(packageJob.indexOf("Extract validated runtime packages")).toBeLessThan(
            packageJob.indexOf("Build and verify exact package set")
        );
    });

    it("persists and consumes the restored runtime package directory", () => {
        expect(testJob).toContain(
            'echo "COPILOT_SDK_RUNTIME_PACKAGE_DIR=$COPILOT_SDK_RUNTIME_PACKAGE_DIR" >> "$GITHUB_ENV"'
        );
        expect(testJob).toContain('echo "COPILOT_CLI_PATH=$runtime_path" >> "$GITHUB_ENV"');
        expect(packageJob).toContain(
            "COPILOT_SDK_RUNTIME_PACKAGE_DIR: ${{ runner.temp }}/runtime-packages"
        );
        expect(packageJob.indexOf("Extract validated runtime packages")).toBeLessThan(
            packageJob.indexOf(
                "COPILOT_SDK_RUNTIME_PACKAGE_DIR: ${{ runner.temp }}/runtime-packages"
            )
        );
    });
});

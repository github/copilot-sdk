import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { assertSafeTestWorkflow } from "../scripts/sdk-canary-safety.js";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflow = (name: string) =>
    readFileSync(join(repositoryRoot, ".github", "workflows", name), "utf8");
const publish = workflow("publish.yml");
const sdkCanary = workflow("sdk-canary.yml");
const publishVersionJob = publish.slice(
    publish.indexOf("  version:"),
    publish.indexOf("  package-nodejs:")
);
const runtimeReleaseIdentity = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "runtime-release-identity.ts"),
    "utf8"
);
const releaseManifest = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "release-manifest.ts"),
    "utf8"
);
const unstableVersion = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "unstable-version.ts"),
    "utf8"
);
const planJob = sdkCanary.slice(
    sdkCanary.indexOf("  plan:"),
    sdkCanary.indexOf("  acquire-runtime:")
);
const acquisitionJob = sdkCanary.slice(
    sdkCanary.indexOf("  acquire-runtime:"),
    sdkCanary.indexOf("  test:")
);
const testJob = sdkCanary.slice(sdkCanary.indexOf("  test:"), sdkCanary.indexOf("  package:"));
const packageJob = sdkCanary.slice(
    sdkCanary.indexOf("  package:"),
    sdkCanary.indexOf("  publish-internal:")
);
const internalPublicationJob = sdkCanary.slice(sdkCanary.indexOf("  publish-internal:"));

function directKeys(source: string, start: string, indent: number): string[] {
    const section = source.slice(source.indexOf(start) + start.length);
    const keyPattern = new RegExp(`^ {${indent}}([a-z][a-z0-9_-]*):\\r?$`, "gm");
    return [...section.matchAll(keyPattern)].map((match) => match[1]);
}

describe("unified publishing workflow contract", () => {
    it("exposes only the approved four inputs", () => {
        expect([...inputSection.matchAll(/^      ([\w-]+):$/gm)].map((match) => match[1])).toEqual([
            "dist-tag",
            "version",
            "mode",
            "runtime",
        ]);
        for (const distTag of ["latest", "prerelease", "unstable", "canary"]) {
            expect(inputSection).toContain(`- ${distTag}`);
        }
        expect(inputSection).toContain('default: "prerelease"');
        expect(inputSection).toContain("default: publish");
        expect(inputSection).toContain("- dry-run");
        expect(existsSync(join(repositoryRoot, ".github", "workflows", "runtime-sdk.yml"))).toBe(
            false
        );
    });

    it("uses repository-wide workflow run IDs for direct unstable identity", () => {
        expect(publishVersionJob).toContain("WORKFLOW_RUN_ID: ${{ github.run_id }}");
        expect(publishVersionJob).not.toContain("WORKFLOW_RUN_NUMBER:");
        expect(unstableVersion).toContain('runId: requireEnvironment("WORKFLOW_RUN_ID")');
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
            expect(publish).toContain(jobName);
        }
    });
});

describe("test-only runtime-driven Node SDK entry contract", () => {
    it("uses only the registered legacy path and workflow_dispatch inputs", () => {
        expect(existsSync(join(repositoryRoot, ".github", "workflows", "runtime-sdk.yml"))).toBe(
            false
        );
        expect(sdkCanary).toContain('name: "TEST ONLY - Runtime-driven Node SDK"');
        expect(sdkCanary).toContain("workflow_dispatch:");
        expect(sdkCanary).not.toMatch(/^\s{2}(?:push|pull_request|schedule|workflow_call):/m);
        const inputSection = sdkCanary.match(/inputs:\r?\n[\s\S]*?\r?\npermissions:/)?.[0];
        if (!inputSection) throw new Error("workflow_dispatch inputs section is missing");
        expect(directKeys(inputSection, "inputs:", 6).sort()).toEqual(
            ["channel", "mode", "runtime_run_id", "runtime_sha", "runtime_version"].sort()
        );
        expect(inputSection).toContain("- tests-only");
        expect(inputSection).toContain("- publish");
        expect(inputSection).toContain("default: publish");
        expect(inputSection).not.toMatch(/^\s+- internal\s*$/m);
    });

    it("contains the exact safe job set with no public write surface", () => {
        expect(directKeys(sdkCanary, "jobs:", 2).sort()).toEqual(
            ["acquire-runtime", "package", "plan", "publish-internal", "test"].sort()
        );
        expect(() => assertSafeTestWorkflow(sdkCanary)).not.toThrow();
        for (const forbidden of [
            "publish-public:",
            "registry.npmjs.org",
            "packages: write",
            "--access public",
            "Update npm for trusted publishing",
        ]) {
            expect(sdkCanary).not.toContain(forbidden);
        }
        expect(sdkCanary).not.toContain("inputs.runtime_source");
        expect(sdkCanary).not.toContain("inputs.version");
        expect(sdkCanary).not.toMatch(/^\s+runtime_source:/m);
        expect(sdkCanary).not.toMatch(/^\s+version:/m);
    });

    it("rejects every unsafe public publication indicator", () => {
        for (const unsafeMutation of [
            "jobs:\n  publish-public:\n",
            "run: npm publish package.tgz",
            "run: npm publish package.tgz --registry https://registry.npmjs.org",
            "run: npm dist-tag add package@1.0.0 test --registry https://registry.npmjs.org",
            "run: |\n  node scripts/npm-release.js publish-manifest \\\n    release-manifest.json dist unstable https://registry.npmjs.org public",
            "run: npm install --global npm@11",
            "permissions:\n  packages: write",
            "run: npm publish package.tgz --access public",
            "name: npm trusted publishing setup",
            "jobs:\n  claim-runtime-dispatch:\n",
            "name: sdk-runtime-test-dispatch-100",
            "run: node scripts/runtime-dispatch-ledger.ts claim",
            "env:\n  CANONICAL_RUN_ID: 200\nrun: gh run watch 200",
            "concurrency:\n  group: sdk-runtime-test-${{ inputs.runtime_run_id }}",
        ]) {
            expect(() => assertSafeTestWorkflow(`${sdkCanary}\n${unsafeMutation}`)).toThrow();
        }
    });

    it("rejects variable-indirected public feeds and modes", () => {
        const publicFeed = sdkCanary.replace(
            "FEED_URL: https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/",
            "FEED_URL: https://registry.npmjs.org"
        );
        expect(() => assertSafeTestWorkflow(publicFeed)).toThrow(
            "exact Azure copilot-canary registry"
        );

        const publicMode = sdkCanary
            .replace("HUSKY: 0", "HUSKY: 0\n  PUBLICATION_MODE: public")
            .replace(
                '"https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/" azure',
                '"https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/" "$PUBLICATION_MODE"'
            );
        expect(() => assertSafeTestWorkflow(publicMode)).toThrow();

        const reassignedFeed = sdkCanary.replace(
            "node nodejs/scripts/npm-release.js publish-manifest",
            "FEED_URL=https://registry.npmjs.org\n          node nodejs/scripts/npm-release.js publish-manifest"
        );
        expect(() => assertSafeTestWorkflow(reassignedFeed)).toThrow(
            "publication feed reassignment"
        );
    });

    it("rejects extra or replaced dispatch inputs", () => {
        const addedRuntimeSource = sdkCanary.replace(
            "      runtime_version:",
            "      runtime_source:\n        required: true\n        type: string\n      runtime_version:"
        );
        expect(() => assertSafeTestWorkflow(addedRuntimeSource)).toThrow(
            "exactly the five approved dispatch inputs"
        );

        const replacedRuntimeRunId = sdkCanary.replace(
            "      runtime_run_id:",
            "      runtime_source:"
        );
        expect(() => assertSafeTestWorkflow(replacedRuntimeRunId)).toThrow(
            "exactly the five approved dispatch inputs"
        );
    });

    it("rejects obsolete internal mode wiring", () => {
        const internalModeOption = sdkCanary.replace("          - publish", "          - internal");
        expect(() => assertSafeTestWorkflow(internalModeOption)).toThrow(
            "modes must be exactly tests-only and publish"
        );

        const internalModeCondition = sdkCanary.replace(
            "inputs.mode == 'publish'",
            "inputs.mode == 'internal'"
        );
        expect(() => assertSafeTestWorkflow(internalModeCondition)).toThrow(
            "must require publish mode"
        );
    });

    it("rejects a missing repository-wide unstable run ID", () => {
        const missingRunId = sdkCanary.replace(
            "          WORKFLOW_RUN_ID: ${{ github.run_id }}\n",
            ""
        );
        expect(() => assertSafeTestWorkflow(missingRunId)).toThrow(
            "repository-wide workflow run ID"
        );
    });

    it("rejects unsafe runtime directory artifact transport", () => {
        const directUpload = sdkCanary.replace(
            "path: ${{ runner.temp }}/runtime-packages.tar.gz",
            "path: ${{ runner.temp }}/runtime-packages"
        );
        expect(() => assertSafeTestWorkflow(directUpload)).toThrow(
            "artifact upload must contain only the validated archive"
        );

        const directDownload = sdkCanary.replace(
            "path: ${{ runner.temp }}/runtime-package-artifact",
            "path: ${{ runner.temp }}/runtime-packages"
        );
        expect(() => assertSafeTestWorkflow(directDownload)).toThrow(
            "test job must download the runtime archive"
        );

        const missingPersistentDirectory = sdkCanary.replace(
            'echo "COPILOT_SDK_RUNTIME_PACKAGE_DIR=$COPILOT_SDK_RUNTIME_PACKAGE_DIR" >> "$GITHUB_ENV"',
            "true"
        );
        expect(() => assertSafeTestWorkflow(missingPersistentDirectory)).toThrow(
            "persist the restored runtime package directory"
        );

        const directWindowsArchivePath = sdkCanary.replace(
            /runner_temp="\$RUNNER_TEMP"\r?\n\s+if command -v cygpath[\s\S]*?fi\r?\n\s+rm -rf "\$runner_temp\/runtime-packages"\r?\n\s+tar -xzf "\$runner_temp\/runtime-package-artifact\/runtime-packages\.tar\.gz" -C "\$runner_temp"/,
            'rm -rf "$RUNNER_TEMP/runtime-packages"\n          tar -xzf "$RUNNER_TEMP/runtime-package-artifact/runtime-packages.tar.gz" -C "$RUNNER_TEMP"'
        );
        expect(() => assertSafeTestWorkflow(directWindowsArchivePath)).toThrow(
            "normalize RUNNER_TEMP for Windows Git Bash"
        );
    });

    it("uses GitHub Packages only for runtime inputs and Azure only for SDK outputs", () => {
        expect(acquisitionJob).toContain("packages: read");
        expect(acquisitionJob).toContain("NODE_AUTH_TOKEN: ${{ github.token }}");
        expect(acquisitionJob).toContain("--registry https://npm.pkg.github.com");
        expect(acquisitionJob).not.toContain("azure/login");
        expect(acquisitionJob).not.toContain("FEED_URL");
        expect(internalPublicationJob).toContain("azure/login");
        expect(sdkCanary).toContain(
            '"https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/" azure'
        );
        expect(internalPublicationJob).not.toContain("npm.pkg.github.com");
        expect(internalPublicationJob).not.toContain("dist.integrity");
        expect(internalPublicationJob).not.toContain("dist.shasum");
        expect(releaseManifest).toContain(
            'assert.equal(manifest.runtime.source, "github-packages", "Invalid runtime package source")'
        );
    });

    it("uses the production mode matrix for isolated Azure publication", () => {
        expect(runtimeReleaseIdentity).toContain('inputs.mode === "tests-only"');
        expect(runtimeReleaseIdentity).toContain('inputs.mode === "publish"');
        expect(runtimeReleaseIdentity).not.toContain('inputs.mode === "internal"');
        expect(runtimeReleaseIdentity).toContain("Invalid channel or mode combination");
        expect(internalPublicationJob).toContain("inputs.mode == 'publish'");
        expect(internalPublicationJob).not.toContain("inputs.mode == 'internal'");
    });

    it("preserves runtime package modes across every artifact boundary", () => {
        expect(acquisitionJob).toContain(
            'tar -czf "$RUNNER_TEMP/runtime-packages.tar.gz" -C "$RUNNER_TEMP" runtime-packages'
        );
        expect(acquisitionJob).toContain("path: ${{ runner.temp }}/runtime-packages.tar.gz");
        expect(acquisitionJob).toContain(
            "name: runtime-test-${{ inputs.channel }}-${{ inputs.runtime_run_id }}-${{ inputs.runtime_version }}-${{ inputs.runtime_sha }}"
        );
        expect(acquisitionJob).not.toContain("path: ${{ runner.temp }}/runtime-packages\n");
        expect(acquisitionJob.indexOf("npm run acquire:runtime-packages")).toBeLessThan(
            acquisitionJob.indexOf("Archive validated runtime packages")
        );
        expect(acquisitionJob.indexOf("Archive validated runtime packages")).toBeLessThan(
            acquisitionJob.indexOf("Upload validated runtime packages")
        );

        for (const consumer of [testJob, packageJob]) {
            expect(consumer).toContain("path: ${{ runner.temp }}/runtime-package-artifact");
            expect(consumer).toContain("if command -v cygpath >/dev/null 2>&1; then");
            expect(consumer).toContain('runner_temp="$(cygpath -u "$runner_temp")"');
            expect(consumer).toContain(
                'tar -xzf "$runner_temp/runtime-package-artifact/runtime-packages.tar.gz" -C "$runner_temp"'
            );
        }
    });

    it("builds one retained package set and preserves runtime publication order", () => {
        expect(runtimePackageJob).toContain("npm run verify:release-packages");
        expect(runtimePackageJob).toContain("release-manifest.json");
        expect(runtimePackageJob).toContain(
            "RUNTIME_RUN_ID: ${{ needs.validate-dispatch.outputs.runtime_run_id }}"
        );
        expect(runtimeInternalJob).toContain("publish-manifest");
        expect(runtimeInternalJob).toContain('"$FEED_URL" azure');
        expect(runtimeInternalJob).toContain("Clean install and package version check");
        expect(runtimePublicJob).toContain("inputs.dist-tag == 'unstable'");
        expect(runtimePublicJob).toContain("needs: [runtime-plan, runtime-publish-internal]");
        expect(runtimePublicJob).toContain("https://registry.npmjs.org public");
    });

    it("executes npm publication directly with shared channel locks", () => {
        expect(directInternalJob).toContain("group: sdk-runtime-internal-${{ inputs.dist-tag }}");
        expect(runtimeInternalJob).toContain("group: sdk-runtime-internal-${{ inputs.dist-tag }}");
        expect(runtimePublicJob).toContain("group: sdk-runtime-public-unstable");
        expect(runtimePublicJob).toContain("id-token: write");
        expect(runtimePublicJob).toContain("npm install --global npm@11.6.3");
        expect(publish).not.toContain("uses: ./.github/workflows/runtime-sdk.yml");
    });

    it("preserves acquisition, three-OS tests, exact packaging, and clean verification", () => {
        expect(sdkCanary).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(sdkCanary).toContain("npm run acquire:runtime-packages");
        expect(planJob.indexOf("Validate runtime release inputs")).toBeLessThan(
            planJob.indexOf("Calculate the collision-resistant test release identity")
        );
        expect(sdkCanary).toMatch(/acquire-runtime:\r?\n[\s\S]*?needs: plan/);
        expect(runtimeReleaseIdentity).toContain("validateRuntimeVersionChannel");
        expect(releaseManifest.match(/validateRuntimeVersionChannel\(/g)).toHaveLength(2);
        expect(sdkCanary).toContain("npm run verify:release-packages");
        expect(sdkCanary).toContain("release:manifest -- create");
        expect(sdkCanary).toContain("release:manifest -- verify");
        expect(sdkCanary).toContain("publish-manifest");
        expect(sdkCanary).not.toContain("for PACKAGE in");
        expect(sdkCanary).toContain(
            'npm install --ignore-scripts "@github/copilot-sdk@${SDK_VERSION}"'
        );
        expect(sdkCanary).toContain("umbrella.copilotCliVersion !== expectedRuntime");
        expect(sdkCanary.indexOf("npm run verify:release-packages")).toBeLessThan(
            sdkCanary.indexOf("publish-manifest")
        );
    });

    it("uses runtime run IDs only as reusable provenance", () => {
        expect(sdkCanary).toContain('description: "Source runtime workflow run ID for provenance"');
        expect(sdkCanary).toContain(
            'run-name: "TEST ONLY - Runtime-driven SDK #${{ github.run_number }} from runtime run ${{ inputs.runtime_run_id }}"'
        );
        expect(planJob).not.toContain("needs:");
        expect(planJob).not.toContain("if: needs.");
        expect(planJob).toContain("npx tsx scripts/runtime-release-identity.ts");
        expect(runtimeReleaseIdentity).toContain("validateRuntimeReleaseInputs");
        for (const forbidden of [
            "claim-runtime-dispatch",
            "sdk-runtime-test-dispatch-",
            "runtime-dispatch-ledger",
            "canonical_run",
            "CANONICAL_RUN_ID",
            "gh run watch",
        ]) {
            expect(sdkCanary).not.toContain(forbidden);
        }
        expect(
            existsSync(join(repositoryRoot, "nodejs", "scripts", "runtime-dispatch-ledger.ts"))
        ).toBe(false);
        expect(
            existsSync(join(repositoryRoot, "nodejs", "test", "runtime-dispatch-ledger.test.ts"))
        ).toBe(false);
    });

    it("isolates tags, versions, artifacts, and serialized state", () => {
        expect(sdkCanary).toContain("runtime-sdk-canary-test");
        expect(sdkCanary).toContain("runtime-sdk-unstable-test");
        expect(sdkCanary).toContain('SDK_VERSION="${SDK_VERSION_BASE}.test.${GITHUB_RUN_ID}"');
        expect(sdkCanary).toContain("SDK_CHANNEL: ${{ inputs.channel }}");
        expect(sdkCanary).not.toContain("scripts/get-version.js current");
        expect(sdkCanary).toContain(
            'WORKFLOW_CREATED_AT="$(gh api "/repos/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID" --jq .created_at)"'
        );
        expect(sdkCanary.indexOf("WORKFLOW_CREATED_AT=")).toBeLessThan(
            sdkCanary.indexOf("scripts/unstable-version.ts")
        );
        expect(sdkCanary).toContain("group: sdk-runtime-test-internal");
        expect(sdkCanary.match(/queue: max/g)).toHaveLength(1);
        expect(sdkCanary).toContain("runtime-test-${{ inputs.channel }}");
        expect(sdkCanary).toContain("cancel-in-progress: false");
        expect(sdkCanary).toContain("!cancelled()");
    });

    it("derives each new SDK identity from its workflow run", () => {
        expect(planJob).toContain("WORKFLOW_RUN_ID: ${{ github.run_id }}");
        expect(sdkCanary).toContain("WORKFLOW_RUN_NUMBER: ${{ github.run_number }}");
        expect(unstableVersion).toContain('runId: requireEnvironment("WORKFLOW_RUN_ID")');
        expect(unstableVersion).toContain('runNumber: requireEnvironment("WORKFLOW_RUN_NUMBER")');
        expect(sdkCanary).toContain("SDK_SHA: ${{ github.sha }}");
        expect(sdkCanary).toContain('SDK_VERSION="${SDK_VERSION_BASE}.test.${GITHUB_RUN_ID}"');
        expect(sdkCanary).not.toMatch(/SDK_VERSION=.*runtime_run_id/i);
        expect(sdkCanary).not.toMatch(/group:.*runtime_run_id/i);
    });
});

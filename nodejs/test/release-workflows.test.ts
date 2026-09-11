import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflowPath = join(repositoryRoot, ".github", "workflows", "publish.yml");
const publish = readFileSync(workflowPath, "utf8");
const jobStart = (name: string) => publish.indexOf(`\n  ${name}:`) + 1;
const job = (name: string, next?: string) =>
    publish.slice(jobStart(name), next ? jobStart(next) : publish.length);

const inputSection = publish.slice(
    publish.indexOf("    inputs:"),
    publish.indexOf("\n\npermissions:")
);
const validateDispatchJob = job("validate-dispatch", "version");
const directVersionJob = job("version", "package-nodejs");
const directPackageJob = job("package-nodejs", "publish-nodejs");
const directPublicJob = job("publish-nodejs", "publish-nodejs-internal");
const directInternalJob = job("publish-nodejs-internal", "publish-dotnet");
const dotnetJob = job("publish-dotnet", "publish-rust");
const rustJob = job("publish-rust", "publish-python");
const pythonJob = job("publish-python", "publish-java");
const javaJob = job("publish-java", "github-release");
const githubReleaseJob = job("github-release", "runtime-plan");
const runtimePlanJob = job("runtime-plan", "runtime-acquire");
const runtimeAcquireJob = job("runtime-acquire", "runtime-test");
const runtimeTestJob = job("runtime-test", "runtime-package");
const runtimePackageJob = job("runtime-package", "runtime-publish-internal");
const runtimeInternalJob = job("runtime-publish-internal", "runtime-publish-public");
const runtimePublicJob = job("runtime-publish-public");
const unstableVersion = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "unstable-version.ts"),
    "utf8"
);

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

    it("parses runtime JSON once and routes only validated outputs", () => {
        expect(validateDispatchJob).toContain("npx tsx scripts/runtime-release-identity.ts");
        expect(validateDispatchJob).toContain("RUNTIME_JSON: ${{ inputs.runtime }}");
        expect(validateDispatchJob).toContain(
            "runtime_version: ${{ steps.validate.outputs.runtime_version }}"
        );
        expect(validateDispatchJob).toContain(
            "runtime_sha: ${{ steps.validate.outputs.runtime_sha }}"
        );
        expect(validateDispatchJob).toContain(
            "runtime_run_id: ${{ steps.validate.outputs.runtime_run_id }}"
        );
        expect(publish).not.toContain("fromJSON(");
        expect(publish).not.toContain("inputs.runtime_version");
        expect(publish).not.toContain("inputs.runtime_sha");
        expect(publish).not.toContain("inputs.runtime_run_id");
        expect(publish).not.toContain("inputs.channel");
        expect(directVersionJob).toContain("if: needs.validate-dispatch.outputs.kind == 'direct'");
        expect(runtimePlanJob).toContain("if: needs.validate-dispatch.outputs.kind == 'runtime'");
    });

    it("keeps dry-runs out of publication locks and mutation jobs", () => {
        expect(publish).toContain(
            "inputs.mode == 'dry-run' && format('publish-dry-run-{0}', github.run_id)"
        );
        expect(directPublicJob).toContain("if: inputs.mode == 'publish'");
        expect(runtimeInternalJob).toContain("inputs.mode == 'publish'");
        expect(runtimePublicJob).toContain("inputs.mode == 'publish'");
        expect(directPackageJob).not.toContain("inputs.mode == 'publish'");
        expect(runtimePackageJob).not.toContain("inputs.mode == 'publish'");
    });
});

describe("direct publishing path", () => {
    it("keeps normal version calculation and deterministic direct unstable planning", () => {
        expect(directVersionJob).toContain(
            "fetch-depth: ${{ inputs.dist-tag == 'unstable' && '0' || '1' }}"
        );
        expect(directVersionJob).toContain("if: inputs.dist-tag == 'unstable'");
        expect(directVersionJob).toContain("WORKFLOW_RUN_ID: ${{ github.run_id }}");
        expect(directVersionJob).toContain("SDK_VERSION_OVERRIDE: ${{ inputs.version }}");
        expect(directVersionJob).toContain("scripts/unstable-version.ts");
        expect(directVersionJob).toContain("if: inputs.dist-tag != 'unstable'");
        expect(directVersionJob).toContain(
            'VERSION="$(node scripts/get-version.js ${{ github.event.inputs.dist-tag }})"'
        );
    });

    it("keeps direct unstable Node-only and public-before-internal", () => {
        expect(directPublicJob).toContain("inputs.dist-tag == 'unstable'");
        expect(directPackageJob).toContain("create-package-set package-set-manifest.json");
        expect(directPublicJob).toContain("publish-manifest");
        expect(directPublicJob).toContain("https://registry.npmjs.org public");
        expect(directInternalJob).toContain("needs: publish-nodejs");
        expect(directInternalJob).toContain("publish-manifest");
        expect(directInternalJob).toContain('"$FEED_URL" azure');
        for (const nonNodeJob of [dotnetJob, rustJob, pythonJob, javaJob, githubReleaseJob]) {
            expect(nonNodeJob).toContain("inputs.dist-tag != 'unstable'");
        }
    });

    it("retains all stable and prerelease publishers", () => {
        for (const jobName of [
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

describe("runtime-backed publishing path", () => {
    it("keeps canary run numbers and unstable repository-wide run IDs", () => {
        expect(runtimePlanJob).toContain("SDK_CHANNEL: ${{ inputs.dist-tag }}");
        expect(runtimePlanJob).toContain("WORKFLOW_RUN_ID: ${{ github.run_id }}");
        expect(runtimePlanJob).toContain("WORKFLOW_RUN_NUMBER: ${{ github.run_number }}");
        expect(unstableVersion).toContain('runId: requireEnvironment("WORKFLOW_RUN_ID")');
        expect(unstableVersion).toContain('runNumber: requireEnvironment("WORKFLOW_RUN_NUMBER")');
    });

    it("acquires validated runtime outputs and tests all runner platforms", () => {
        expect(runtimeAcquireJob).toContain("packages: read");
        expect(runtimeAcquireJob).toContain(
            "RUNTIME_SHA: ${{ needs.validate-dispatch.outputs.runtime_sha }}"
        );
        expect(runtimeAcquireJob).toContain(
            "RUNTIME_VERSION: ${{ needs.validate-dispatch.outputs.runtime_version }}"
        );
        expect(runtimeAcquireJob).toContain("npm run acquire:runtime-packages");
        expect(runtimeAcquireJob).not.toContain("azure/login");
        expect(runtimeTestJob).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(runtimeTestJob).toContain("npm test");
    });

    it("preserves executable modes across runtime package artifact boundaries", () => {
        expect(runtimeAcquireJob).toContain(
            'tar -czf "$RUNNER_TEMP/runtime-packages.tar.gz" -C "$RUNNER_TEMP" runtime-packages'
        );
        for (const consumer of [runtimeTestJob, runtimePackageJob]) {
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
});

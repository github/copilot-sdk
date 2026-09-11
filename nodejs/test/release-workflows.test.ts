import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { assertSafeTestWorkflow } from "../scripts/sdk-canary-safety.js";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflow = (name: string) =>
    readFileSync(join(repositoryRoot, ".github", "workflows", name), "utf8");
const publish = workflow("publish.yml");
const sdkCanary = workflow("sdk-canary.yml");
const runtimeReleaseIdentity = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "runtime-release-identity.ts"),
    "utf8"
);
const runtimePackageAcquisition = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "runtime-package-acquisition.ts"),
    "utf8"
);
const unstableVersion = readFileSync(
    join(repositoryRoot, "nodejs", "scripts", "unstable-version.ts"),
    "utf8"
);

function job(source: string, name: string, next?: string): string {
    const start = source.indexOf(`  ${name}:`);
    if (start < 0) throw new Error(`Missing job: ${name}`);
    const end = next ? source.indexOf(`  ${next}:`, start) : source.length;
    if (end < 0) throw new Error(`Missing following job: ${next}`);
    return source.slice(start, end);
}

function directKeys(source: string, start: string, indent: number): string[] {
    const section = source.slice(source.indexOf(start) + start.length);
    const keyPattern = new RegExp(`^ {${indent}}([a-z][a-z0-9_-]*):\\r?$`, "gm");
    return [...section.matchAll(keyPattern)].map((match) => match[1]);
}

const publishInputs = publish.match(/inputs:\r?\n[\s\S]*?\r?\npermissions:/)?.[0];
if (!publishInputs) throw new Error("publish workflow inputs section is missing");
const testInputs = sdkCanary.match(/inputs:\r?\n[\s\S]*?\r?\npermissions:/)?.[0];
if (!testInputs) throw new Error("test workflow inputs section is missing");

const productionValidateJob = job(publish, "validate-dispatch", "version");
const directVersionJob = job(publish, "version", "package-nodejs");
const directPackageJob = job(publish, "package-nodejs", "publish-nodejs");
const directPublicJob = job(publish, "publish-nodejs", "publish-nodejs-internal");
const directInternalJob = job(publish, "publish-nodejs-internal", "publish-dotnet");
const dotnetJob = job(publish, "publish-dotnet", "publish-rust");
const rustJob = job(publish, "publish-rust", "publish-python");
const pythonJob = job(publish, "publish-python", "publish-java");
const javaJob = job(publish, "publish-java", "github-release");
const githubReleaseJob = job(publish, "github-release", "runtime-plan");
const runtimePlanJob = job(publish, "runtime-plan", "runtime-acquire");
const runtimeAcquireJob = job(publish, "runtime-acquire", "runtime-test");
const runtimeTestJob = job(publish, "runtime-test", "runtime-package");
const runtimePackageJob = job(publish, "runtime-package", "runtime-publish-internal");
const runtimeInternalJob = job(publish, "runtime-publish-internal", "runtime-publish-public");
const runtimePublicJob = job(publish, "runtime-publish-public");

const testValidateJob = job(sdkCanary, "validate-dispatch", "plan");
const planJob = job(sdkCanary, "plan", "acquire-runtime");
const acquisitionJob = job(sdkCanary, "acquire-runtime", "test");
const testJob = job(sdkCanary, "test", "package");
const packageJob = job(sdkCanary, "package", "publish-internal");
const internalPublicationJob = job(sdkCanary, "publish-internal");

describe("unified publishing workflow contract", () => {
    it("exposes only the approved four inputs", () => {
        expect(directKeys(publishInputs, "inputs:", 6)).toEqual([
            "dist-tag",
            "version",
            "mode",
            "runtime",
        ]);
        for (const distTag of ["latest", "prerelease", "unstable", "canary"]) {
            expect(publishInputs).toContain(`- ${distTag}`);
        }
        expect(publishInputs).toContain('default: "prerelease"');
        expect(publishInputs).toContain("default: publish");
        expect(publishInputs).toContain("- dry-run");
        expect(existsSync(join(repositoryRoot, ".github", "workflows", "runtime-sdk.yml"))).toBe(
            false
        );
    });

    it("parses runtime JSON once and routes only validated outputs", () => {
        expect(productionValidateJob).toContain("npx tsx scripts/runtime-release-identity.ts");
        expect(productionValidateJob).toContain("RUNTIME_JSON: ${{ inputs.runtime }}");
        expect(productionValidateJob).toContain(
            "runtime_version: ${{ steps.validate.outputs.runtime_version }}"
        );
        expect(productionValidateJob).toContain(
            "runtime_sha: ${{ steps.validate.outputs.runtime_sha }}"
        );
        expect(productionValidateJob).toContain(
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

describe("test-only runtime-driven Node SDK entry contract", () => {
    it("uses exactly the runtime-backed three-input contract", () => {
        expect(sdkCanary).toContain('name: "TEST ONLY - Runtime-driven Node SDK"');
        expect(sdkCanary).toContain("workflow_dispatch:");
        expect(sdkCanary).not.toMatch(/^\s{2}(?:push|pull_request|schedule|workflow_call):/m);
        expect(directKeys(testInputs, "inputs:", 6)).toEqual(["dist-tag", "mode", "runtime"]);
        expect(testInputs).toContain("- canary");
        expect(testInputs).toContain("- unstable");
        expect(testInputs).toContain("- dry-run");
        expect(testInputs).toContain("- publish");
        expect(testInputs).toContain("default: dry-run");
        expect(testInputs).toMatch(/runtime:\r?\n\s+description:[\s\S]*?\r?\n\s+required: true/);
        for (const obsolete of [
            "channel:",
            "runtime_version:",
            "runtime_sha:",
            "runtime_run_id:",
            "tests-only",
            "runtime-sdk.yml",
        ]) {
            expect(testInputs).not.toContain(obsolete);
        }
    });

    it("contains only the safe runtime job set", () => {
        expect(directKeys(sdkCanary, "jobs:", 2).sort()).toEqual(
            [
                "acquire-runtime",
                "package",
                "plan",
                "publish-internal",
                "test",
                "validate-dispatch",
            ].sort()
        );
        expect(() => assertSafeTestWorkflow(sdkCanary)).not.toThrow();
        for (const forbidden of [
            "publish-public:",
            "runtime-publish-public:",
            "registry.npmjs.org",
            "packages: write",
            "--access public",
            "Update npm for trusted publishing",
            "gh release create",
            "inputs.version",
        ]) {
            expect(sdkCanary).not.toContain(forbidden);
        }
    });

    it("validates required runtime JSON before planning and acquisition", () => {
        expect(testValidateJob).toContain("npx tsx scripts/runtime-release-identity.ts");
        expect(testValidateJob).toContain("DIST_TAG: ${{ inputs.dist-tag }}");
        expect(testValidateJob).toContain("MODE: ${{ inputs.mode }}");
        expect(testValidateJob).toContain("RUNTIME_JSON: ${{ inputs.runtime }}");
        expect(testValidateJob).toContain('VERSION_OVERRIDE: ""');
        expect(testValidateJob).toContain('= "runtime"');
        expect(planJob).toContain("needs: validate-dispatch");
        expect(acquisitionJob).toContain("needs: [validate-dispatch, plan]");
        expect(runtimeReleaseIdentity).toContain(
            "Runtime input must contain exactly version, sha, and run_id"
        );
        expect(runtimeReleaseIdentity).toContain("validateRuntimeVersionChannel");
    });

    it("rejects unsafe inputs, modes, and public or release mutations", () => {
        for (const unsafeMutation of [
            "jobs:\n  publish-public:\n",
            "jobs:\n  runtime-publish-public:\n",
            "run: npm publish package.tgz",
            "run: npm dist-tag add package@1.0.0 test --registry https://registry.npmjs.org",
            "run: npm install --global npm@11",
            "permissions:\n  packages: write",
            "run: npm publish package.tgz --access public",
            "run: gh release create v1.2.3",
            "run: git tag v1.2.3",
            "uses: ./.github/workflows/runtime-sdk.yml",
            "inputs:\n      runtime_version:\n",
            "inputs.mode == 'internal'",
            "          - tests-only",
        ]) {
            expect(() => assertSafeTestWorkflow(`${sdkCanary}\n${unsafeMutation}`)).toThrow();
        }
    });

    it("rejects variable-indirected public feeds and publication modes", () => {
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
    });

    it("uses GitHub Packages only for runtime input and Azure only for SDK output", () => {
        expect(acquisitionJob).toContain("packages: read");
        expect(acquisitionJob).toContain("NODE_AUTH_TOKEN: ${{ github.token }}");
        expect(acquisitionJob).not.toContain("--registry");
        expect(runtimePackageAcquisition).toContain(
            'const GITHUB_PACKAGES_REGISTRY = "https://npm.pkg.github.com";'
        );
        expect(acquisitionJob).not.toContain("azure/login");
        expect(acquisitionJob).not.toContain("FEED_URL");
        expect(internalPublicationJob).toContain("azure/login");
        expect(internalPublicationJob).toContain("inputs.mode == 'publish'");
        expect(internalPublicationJob).toContain(
            '"https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/" azure'
        );
        expect(internalPublicationJob).not.toContain("npm.pkg.github.com");
    });

    it("preserves runtime package modes across every artifact boundary", () => {
        expect(acquisitionJob).toContain(
            'tar -czf "$RUNNER_TEMP/runtime-packages.tar.gz" -C "$RUNNER_TEMP" runtime-packages'
        );
        expect(acquisitionJob).toContain("path: ${{ runner.temp }}/runtime-packages.tar.gz");
        for (const consumer of [testJob, packageJob]) {
            expect(consumer).toContain("path: ${{ runner.temp }}/runtime-package-artifact");
            expect(consumer).toContain("if command -v cygpath >/dev/null 2>&1; then");
            expect(consumer).toContain('runner_temp="$(cygpath -u "$runner_temp")"');
            expect(consumer).toContain(
                'tar -xzf "$runner_temp/runtime-package-artifact/runtime-packages.tar.gz" -C "$runner_temp"'
            );
        }
        expect(testJob).toContain(
            'echo "COPILOT_SDK_RUNTIME_PACKAGE_DIR=$COPILOT_SDK_RUNTIME_PACKAGE_DIR" >> "$GITHUB_ENV"'
        );
        expect(testJob).toContain('echo "COPILOT_CLI_PATH=$runtime_path" >> "$GITHUB_ENV"');
        expect(packageJob).toContain(
            "COPILOT_SDK_RUNTIME_PACKAGE_DIR: ${{ runner.temp }}/runtime-packages"
        );
    });

    it("keeps collision-resistant test versions and isolated Azure tags", () => {
        expect(planJob).toContain("SDK_CHANNEL: ${{ inputs.dist-tag }}");
        expect(planJob).toContain("WORKFLOW_RUN_ID: ${{ github.run_id }}");
        expect(planJob).toContain("WORKFLOW_RUN_NUMBER: ${{ github.run_number }}");
        expect(planJob).toContain('SDK_VERSION="${SDK_VERSION_BASE}.test.${GITHUB_RUN_ID}"');
        expect(unstableVersion).toContain('runId: requireEnvironment("WORKFLOW_RUN_ID")');
        expect(sdkCanary).toContain("runtime-sdk-canary-test");
        expect(sdkCanary).toContain("runtime-sdk-unstable-test");
        expect(sdkCanary).toContain("group: sdk-runtime-test-internal");
    });

    it("builds and verifies one immutable nine-package release before publication", () => {
        expect(packageJob).toContain("npm run verify:release-packages");
        expect(packageJob).toContain("release:manifest -- create");
        expect(packageJob).toContain("release:manifest -- verify");
        expect(packageJob).toContain(
            "RUNTIME_RUN_ID: ${{ needs.validate-dispatch.outputs.runtime_run_id }}"
        );
        expect(internalPublicationJob).toContain("publish-manifest");
        expect(internalPublicationJob).toContain("Clean install and package version check");
        expect(sdkCanary.indexOf("npm run verify:release-packages")).toBeLessThan(
            sdkCanary.indexOf("publish-manifest")
        );
    });
});

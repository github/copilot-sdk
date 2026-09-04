import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const canary = readFileSync(join(repositoryRoot, ".github", "workflows", "sdk-canary.yml"), "utf8");
const publish = readFileSync(join(repositoryRoot, ".github", "workflows", "publish.yml"), "utf8");

describe("SDK canary workflow contract", () => {
    it("accepts only the exact Azure canary handoff", () => {
        for (const input of [
            "channel:",
            "runtime_version:",
            "runtime_sha:",
            "runtime_source:",
            "runtime_run_id:",
            "mode:",
        ]) {
            expect(canary).toContain(input);
        }
        expect(canary).toContain("- canary");
        expect(canary).toContain("- azure");
        expect(canary).toContain("- tests-only");
        expect(canary).toContain("- internal");
        expect(canary).not.toContain("registry.npmjs.org");
        expect(canary).not.toContain("npm.pkg.github.com");
    });

    it("tests all hosts and packages before optional internal publication", () => {
        expect(canary).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(canary).toContain("npm run acquire:runtime-packages");
        expect(canary).toContain("npm run verify:release-packages");
        expect(canary).toContain("publish-manifest");
        expect(canary.indexOf("npm run verify:release-packages")).toBeLessThan(
            canary.indexOf("publish-manifest")
        );
    });
});

describe("unstable publishing workflow contract", () => {
    it("requires the authenticated GitHub Packages runtime handoff", () => {
        expect(publish).toContain("runtime_source:");
        expect(publish).toContain("- github-packages");
        expect(publish).toContain("packages: read");
        expect(publish).toContain("//npm.pkg.github.com/:_authToken=");
        expect(publish).not.toContain("@github:registry=https://npm.pkg.github.com");
    });

    it("freezes, tests, packages once, then publishes internal-first", () => {
        expect(publish).toContain("scripts/unstable-version.ts");
        expect(publish).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(publish).toContain("release-manifest.json");
        expect(publish).toContain("COPILOT_CLI_USE_NPM_PACKAGE = false");
        expect(publish.indexOf("unstable-publish-internal:")).toBeLessThan(
            publish.indexOf("unstable-publish-public:")
        );
        expect(publish).toContain("needs: [unstable-plan, unstable-publish-internal]");
    });

    it("supports retained-artifact recovery without enabling non-Node release paths", () => {
        expect(publish).toContain("resume_run_id:");
        expect(publish).toContain("run-id: ${{ inputs.resume_run_id }}");
        expect(publish).toContain("Manifest workflow run ID does not match resume_run_id");
        expect(
            publish.match(/github\.event\.inputs\.dist-tag != 'unstable'/g)?.length
        ).toBeGreaterThan(3);
        expect(publish).toContain("github.event.inputs.dist-tag != 'unstable' &&");
    });
});
